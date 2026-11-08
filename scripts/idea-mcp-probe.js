// One-off probe: MCP SSE handshake against JetBrains IDEA MCP server.
const http = require('http');
const BASE = process.env.MCP_BASE || 'http://127.0.0.1:64342';

let endpoint = null;
let buffer = '';
let endpointResolve;
const endpointPromise = new Promise((r) => (endpointResolve = r));
let inbox = [];
let waiters = [];

function onEvent(raw) {
  const lines = raw.split(/\r?\n/);
  let event = 'message';
  let data = '';
  for (const line of lines) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    else if (line.startsWith('data:')) data += line.slice(5).trim();
  }
  if (event === 'endpoint') {
    endpoint = data.startsWith('http') ? data : BASE + data;
    endpointResolve(endpoint);
    return;
  }
  if (event === 'message' && data) {
    try {
      const msg = JSON.parse(data);
      inbox.push(msg);
      waiters = waiters.filter((w) => (w(msg) ? false : true));
    } catch (e) {
      console.log('[unparsed sse data]', data.slice(0, 300));
    }
  }
}

function waitFor(id, timeoutMs = 15000) {
  const hit = inbox.find((m) => m.id === id);
  if (hit) return Promise.resolve(hit);
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('timeout waiting for id=' + id)), timeoutMs);
    waiters.push((msg) => {
      if (msg.id === id) {
        clearTimeout(timer);
        resolve(msg);
        return true;
      }
      return false;
    });
  });
}

function post(sessionPath, payload) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(payload);
    const req = http.request(
      BASE + sessionPath,
      { method: 'POST', headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) } },
      (res) => {
        let body = '';
        res.on('data', (c) => (body += c));
        res.on('end', () => resolve({ status: res.statusCode, body }));
      }
    );
    req.on('error', reject);
    req.end(data);
  });
}

setTimeout(() => {
  console.log('[hard-timeout] endpoint=%s inbox=%d bufferLen=%d', endpoint, inbox.length, buffer.length);
  process.exit(3);
}, 20000).unref();

(async () => {
  const sseReq = http.get(BASE + '/sse', { headers: { Accept: 'text/event-stream' } }, (res) => {
    console.log('[sse] status=%s content-type=%s', res.statusCode, res.headers['content-type']);
    res.setEncoding('utf8');
    res.on('data', (chunk) => {
      buffer += chunk.replace(/\r\n/g, '\n');
      let idx;
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        onEvent(buffer.slice(0, idx));
        buffer = buffer.slice(idx + 2);
      }
    });
  });
  sseReq.on('error', (e) => console.log('[sse] error:', e.message));

  const ep = await endpointPromise;
  console.log('[sse] endpoint =', ep);
  const path = ep.replace(BASE, '');

  const init = await post(path, {
    jsonrpc: '2.0',
    id: 1,
    method: 'initialize',
    params: {
      protocolVersion: '2025-06-18',
      capabilities: {},
      clientInfo: { name: 'dsh-probe', version: '0.0.1' },
    },
  });
  console.log('[post initialize] http', init.status, init.body.slice(0, 100));
  const initMsg = await waitFor(1);
  console.log('[initialize result]', JSON.stringify(initMsg.result || initMsg.error, null, 2).slice(0, 2000));

  await post(path, { jsonrpc: '2.0', method: 'notifications/initialized' });

  const list = await post(path, { jsonrpc: '2.0', id: 2, method: 'tools/list' });
  console.log('[post tools/list] http', list.status);
  const listMsg = await waitFor(2);
  const tools = (listMsg.result && listMsg.result.tools) || [];
  console.log('[tools count]', tools.length);
  console.log('[tool names]', tools.map((t) => t.name).join(', '));
  if (tools.length) {
    console.log('[first tool schema]', JSON.stringify(tools[0], null, 2).slice(0, 1200));
  }

  const call = await post(path, {
    jsonrpc: '2.0',
    id: 3,
    method: 'tools/call',
    params: { name: process.env.MCP_TOOL || 'get_project_modules', arguments: {} },
  });
  console.log('[post tools/call] http', call.status);
  const callMsg = await waitFor(3, 30000);
  console.log('[tool call result]', JSON.stringify(callMsg.result || callMsg.error, null, 2).slice(0, 3000));

  sseReq.destroy();
  process.exit(0);
})().catch((e) => {
  console.log('[fatal]', e.message);
  process.exit(2);
});
