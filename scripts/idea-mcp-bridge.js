#!/usr/bin/env node
/**
 * stdio <-> SSE bridge for the IntelliJ IDEA built-in MCP Server.
 *
 * DSH's @deepseek-ai/dsh-mcp-client speaks MCP over stdio, while IDEA 2025.3
 * only exposes the legacy SSE transport (GET /sse + POST /message?sessionId=).
 * This bridge spawns nothing and has no dependencies beyond Node built-ins.
 *
 * Usage: node idea-mcp-bridge.js [sseUrl]
 *   sseUrl defaults to $IDEA_MCP_SSE_URL or http://127.0.0.1:64342/sse
 *
 * stdout carries MCP JSON-RPC messages only (newline delimited).
 * All diagnostics go to stderr.
 */

const http = require('http');
const readline = require('readline');

const SSE_URL = process.argv[2] || process.env.IDEA_MCP_SSE_URL || 'http://127.0.0.1:64342/sse';
const BASE = new URL(SSE_URL).origin;

function log(...args) {
  process.stderr.write('[idea-mcp-bridge] ' + args.join(' ') + '\n');
}

let endpointPath = null;
let sessionId = null;
let sseResponse = null;
const pending = []; // JSON-RPC messages received before the session endpoint was known
let buffer = '';
const toolOutputSchemas = new Map(); // tool name -> advertised outputSchema
const pendingCalls = new Map(); // JSON-RPC id -> tool name

/**
 * IDEA 2025.3.2 omits optional-looking output fields (e.g.
 * `probablyHasMoreMatchingFiles`) from structuredContent when they are `false`,
 * even though its own outputSchema marks them required. Strict clients then
 * reject an otherwise successful call. Fill only the missing required
 * top-level fields with type-appropriate defaults; never overwrite real values.
 */
function defaultValueFor(schema) {
  let type = schema && schema.type;
  if (Array.isArray(type)) type = type.find((t) => t !== 'null');
  switch (type) {
    case 'boolean': return false;
    case 'integer':
    case 'number': return 0;
    case 'string': return '';
    case 'array': return [];
    case 'object': return {};
    default: return null;
  }
}

function normalizeResult(toolName, msg) {
  const outputSchema = toolOutputSchemas.get(toolName);
  const structured = msg.result && msg.result.structuredContent;
  if (!outputSchema || !structured || typeof structured !== 'object') return msg;
  const required = outputSchema.required || [];
  const filled = [];
  for (const key of required) {
    if (!(key in structured) && outputSchema.properties && outputSchema.properties[key]) {
      structured[key] = defaultValueFor(outputSchema.properties[key]);
      filled.push(key);
    }
  }
  if (filled.length) log('filled missing structuredContent fields for ' + toolName + ': ' + filled.join(','));
  return msg;
}

function prepare(msg) {
  if (!msg || typeof msg !== 'object') return msg;
  if (msg.result && Array.isArray(msg.result.tools)) {
    for (const tool of msg.result.tools) {
      if (tool && tool.name && tool.outputSchema) toolOutputSchemas.set(tool.name, tool.outputSchema);
    }
  }
  if (msg.id !== undefined && pendingCalls.has(msg.id)) {
    const toolName = pendingCalls.get(msg.id);
    pendingCalls.delete(msg.id);
    if (!msg.error) normalizeResult(toolName, msg);
  }
  return msg;
}


function emit(message) {
  process.stdout.write(JSON.stringify(message) + '\n');
}

function emitError(id, code, message) {
  emit({ jsonrpc: '2.0', id, error: { code, message } });
}

function post(message) {
  const body = JSON.stringify(message);
  const req = http.request(
    BASE + endpointPath,
    { method: 'POST', headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) } },
    (res) => {
      res.resume();
      if (res.statusCode >= 400) {
        log('POST failed with HTTP ' + res.statusCode);
        if (typeof message.id !== 'undefined') {
          emitError(message.id, -32000, 'IDE MCP endpoint rejected the message (HTTP ' + res.statusCode + ')');
        }
      }
    }
  );
  req.on('error', (err) => {
    log('POST error: ' + err.message);
    if (typeof message.id !== 'undefined') {
      emitError(message.id, -32000, 'IDE MCP endpoint unreachable: ' + err.message);
    }
  });
  req.end(body);
}

function dispatch(message) {
  if (message && message.method === 'tools/call' && message.id !== undefined && message.params) {
    pendingCalls.set(message.id, message.params.name);
  }
  if (!endpointPath) {
    pending.push(message);
    return;
  }
  post(message);
}

function handleEvent(raw) {
  const lines = raw.split(/\n/);
  let event = 'message';
  let data = '';
  for (const line of lines) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    else if (line.startsWith('data:')) data += line.slice(5).trim();
  }
  if (event === 'endpoint') {
    endpointPath = data.startsWith('http') ? new URL(data).pathname + new URL(data).search : data;
    const sid = /sessionId=([^&]+)/.exec(data);
    sessionId = sid ? sid[1] : null;
    log('session endpoint ready: ' + endpointPath);
    while (pending.length) post(pending.shift());
    return;
  }
  if (event === 'message' && data) {
    try {
      emit(prepare(JSON.parse(data)));
    } catch (err) {
      log('dropping unparsable SSE payload');
    }
  }
}

function connect() {
  const req = http.get(SSE_URL, { headers: { Accept: 'text/event-stream' } }, (res) => {
    sseResponse = res;
    log('SSE connected: HTTP ' + res.statusCode);
    if (res.statusCode !== 200) {
      log('SSE handshake failed, exiting so the client can reconnect');
      process.exit(1);
    }
    res.setEncoding('utf8');
    res.on('data', (chunk) => {
      buffer += chunk.replace(/\r\n/g, '\n');
      let idx;
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        handleEvent(buffer.slice(0, idx));
        buffer = buffer.slice(idx + 2);
      }
    });
    res.on('end', () => {
      log('SSE stream ended');
      process.exit(1);
    });
    res.on('error', (err) => {
      log('SSE stream error: ' + err.message);
      process.exit(1);
    });
  });
  req.on('error', (err) => {
    log('SSE connect error: ' + err.message);
    process.exit(1);
  });
}

const rl = readline.createInterface({ input: process.stdin, crlfDelay: Infinity });
rl.on('line', (line) => {
  const text = line.trim();
  if (!text) return;
  let message;
  try {
    message = JSON.parse(text);
  } catch (err) {
    log('stdin line is not JSON, ignored');
    return;
  }
  dispatch(message);
});

process.on('SIGINT', () => process.exit(0));
process.on('SIGTERM', () => process.exit(0));

connect();
