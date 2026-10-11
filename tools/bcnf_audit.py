#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
bcnf_audit.py —— 用自己交付的真实表结构做 BCNF 体检

输入:
  --ddl      真实建表脚本(src/main/resources/sql/airline_company.sql),只从中取
             列名与主码/唯一约束 —— 保证"体检对象"是真的,不是手抄的
  --schema   逻辑关系模式 + 语义函数依赖(手写输入,FD 是语义约束,DDL 里写不全)
  --outdir   体检报告输出目录,缺省 docs/

算法(教科书原版,没有任何近似):
  1. 属性闭包 X+ :反复扫描 FD,左边被包含就把右边并进来,直到不动
  2. 候选键     :枚举属性子集,取 X+ = 全属性 的极小者;主属性 = 所有候选键的并集
  3. 范式判定   :对每条非平凡 FD X -> Y
                    X 是超键              -> OK(BCNF)
                    X 不是超键, Y 是主属性 -> 3NF 的逃生口, 违反 BCNF
                    X 不是超键, Y 非主属性 -> 违反 3NF(也违反 BCNF)
  4. 分解建议   :BCNF 分解算法 —— 违反项 X->Y 拆成 (X∪Y) 与 (R−Y)
  5. 无损连接   :分解后每个子模式的候选键 must 覆盖 原关系 的每个键;
                 两两分解用 R1∩R2 -> (R1−R2) 或 R1∩R2 -> (R2−R1) 判定
  6. 保持依赖   :原 FD 集合的闭包 是否被各子模式 FD 的并集闭包覆盖

用法:
  python tools/bcnf_audit.py
  python tools/bcnf_audit.py --outdir docs
"""

from __future__ import annotations

import argparse
import itertools
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent

# ============================================================
# 1. FD 工具:属性闭包 / 候选键 / 覆盖判定
# ============================================================


def closure(attrs, fds):
    """属性闭包 X+。attrs 可为集合;返回 frozenset。"""
    result = set(attrs)
    changed = True
    while changed:
        changed = False
        for lhs, rhs in fds:
            if set(lhs) <= result and not set(rhs) <= result:
                result |= set(rhs)
                changed = True
    return frozenset(result)


def is_superkey(attrs, all_attrs, fds):
    return closure(attrs, fds) == frozenset(all_attrs)


def all_candidate_keys(all_attrs, fds):
    """枚举子集求极小超键。属性数 <= 12 时够用(2^12)。"""
    attrs = sorted(all_attrs)
    n = len(attrs)
    keys = []
    for size in range(1, n + 1):
        for combo in itertools.combinations(attrs, size):
            combo = frozenset(combo)
            if any(k <= combo for k in keys):
                continue
            if is_superkey(combo, all_attrs, fds):
                keys.append(combo)
    return keys


def fd_implied(lhs, rhs, fds):
    return set(rhs) <= closure(lhs, fds)


def fd_set_equivalent(fds_a, fds_b):
    """fds_b 是否蕴含 fds_a 的每一条(用于"保持依赖"判定)。"""
    return all(fd_implied(lhs, rhs, list(fds_b)) for lhs, rhs in fds_a)


def closure_of_fd_set(fds):
    """把 FD 集合闭包化(所有可推导的 FD),仅供展示。"""
    attrs = sorted({a for lhs, rhs in fds for a in list(lhs) + list(rhs)})
    out = []
    for size in range(1, len(attrs) + 1):
        for combo in itertools.combinations(attrs, size):
            lhs = frozenset(combo)
            cl = closure(lhs, fds)
            rhs = set(cl) - set(lhs)
            if rhs:
                out.append((lhs, frozenset(rhs)))
    return out


# ============================================================
# 2. 关系模式与语义 FD 的定义
# ============================================================


@dataclass
class Rel:
    name: str
    attrs: list
    fds: list
    note: str = ""
    declared_keys: list = field(default_factory=list)   # DDL 里真实存在的主码/唯一约束
    derived: bool = False                               # True = 从真实表退化/连接出来的


AIRLINE_DDL = ROOT / "src/main/resources/sql/airline_company.sql"
ONLINE_BOOKSTORE_DDL = ROOT / "src/main/resources/sql/online_bookstore.sql"

SCHEMA = [
    # ---------- 真实表:customer 的三值逻辑边界 ----------
    Rel(
        "customer",
        ["customer_id", "name", "phone", "email", "address"],
        [
            (["customer_id"], ["name", "phone", "email", "address"]),
            (["email"], ["customer_id", "name", "phone", "address"]),
        ],
        declared_keys=["PRIMARY KEY (customer_id)", "UNIQUE (email)"],
        note="email 可空 => UNIQUE(email) 只约束非空值, SQL 语义下允许多行 NULL 并存",
    ),
    # ---------- 真实表:route / route_leg ----------
    Rel(
        "route",
        ["route_no", "route_name"],
        [(["route_no"], ["route_name"])],
        declared_keys=["PRIMARY KEY (route_no)"],
    ),
    Rel(
        "route_leg",
        ["route_no", "leg_no", "from_code", "to_code", "distance_km"],
        [
            (["route_no", "leg_no"], ["from_code", "to_code", "distance_km"]),
            (["route_no", "to_code"], ["leg_no", "from_code", "distance_km"]),
        ],
        declared_keys=["PRIMARY KEY (route_no, leg_no)"],
        note="语义追加一条:同一条航线里到达机场不重复 => 也是候选键"
             "(已由可延迟约束触发器 assert_route_legs_continuous 的连续编号 + 首尾相接蕴含, 不是额外约束)",
    ),
    # ---------- 真实表:timetable(候选键冗余的活标本) ----------
    Rel(
        "timetable",
        ["schedule_id", "flight_no", "route_no", "type_code",
         "day_of_week", "dep_time", "arr_time", "valid_from", "valid_to"],
        [
            (["schedule_id"],
             ["flight_no", "route_no", "type_code", "day_of_week",
              "dep_time", "arr_time", "valid_from", "valid_to"]),
            (["flight_no", "day_of_week", "valid_from"],
             ["schedule_id", "route_no", "type_code",
              "dep_time", "arr_time", "valid_to"]),
            (["flight_no", "day_of_week", "route_no"], ["schedule_id"]),
        ],
        declared_keys=["PRIMARY KEY (schedule_id)",
                       "UNIQUE (flight_no, day_of_week, valid_from)"],
        note="代理键 + 自然键并存:两条候选键,非主属性互相不决定,处于 BCNF",
    ),
    # ---------- 真实表:flight(三层设计的主体) ----------
    Rel(
        "flight",
        ["flight_no", "flight_date", "schedule_id", "reg_no",
         "dep_time", "arr_time", "flight_status"],
        [
            (["flight_no", "flight_date"],
             ["schedule_id", "reg_no", "dep_time", "arr_time", "flight_status"]),
        ],
        declared_keys=["PRIMARY KEY (flight_no, flight_date)"],
        note="实际起降时刻 dep_time/arr_time 与时刻表可能不同(延误), 故不派生自 timetable",
    ),
    # ---------- 真实表:booking ----------
    Rel(
        "booking",
        ["booking_id", "customer_id", "booking_date", "total_amount", "booking_status"],
        [
            (["booking_id"],
             ["customer_id", "booking_date", "total_amount", "booking_status"]),
        ],
        declared_keys=["PRIMARY KEY (booking_id)"],
    ),
    # ---------- 真实表:booking_segment(本次体检的主角) ----------
    Rel(
        "booking_segment",
        ["booking_id", "seg_no", "flight_no", "flight_date",
         "cabin_class", "seat_no", "segment_status"],
        [
            (["booking_id", "seg_no"],
             ["flight_no", "flight_date", "cabin_class", "seat_no", "segment_status"]),
            # 语义追加:座位号(非空时)决定舱位 —— 这是"隐藏的 BCNF 违反"
            (["seat_no"], ["cabin_class"]),
        ],
        declared_keys=["PRIMARY KEY (booking_id, seg_no)"],
        note="seat_no 可空, 唯一索引带 WHERE 条件 => FD 只在子集上成立; 语义上 seat_no->cabin_class 会违反 BCNF",
    ),
    # ---------- 派生:把三层设计"拍平"成一张宽表(用于对照,不是你的表) ----------
    Rel(
        "flight_wide_season  (派生对照,非真实表)",
        ["flight_no", "flight_date", "route_no", "type_code", "seat_capacity",
         "dep_time", "arr_time", "flight_status"],
        [
            (["flight_no"], ["route_no", "type_code", "seat_capacity", "dep_time", "arr_time"]),
            (["flight_no", "flight_date"],
             ["route_no", "type_code", "seat_capacity", "dep_time", "arr_time", "flight_status"]),
        ],
        derived=True,
        note="flight ⋈ timetable ⋈ aircraft_type 拍平; 冗余来自 timetable 的路线/机型/时列表述",
    ),
]


# ============================================================
# 3. 从真实 DDL 里提取列与约束(体检对象必须来自磁盘上的文件)
# ============================================================


def parse_ddl(path: Path):
    """返回 {表名: {"columns": [列], "constraints": [约束原文]}}"""
    text = path.read_text(encoding="utf-8")
    out = {}
    for m in re.finditer(r"CREATE TABLE\s+(\w+)\s*\((.*?)\n\);", text, re.S):
        table = m.group(1)
        body = m.group(2)
        cols, cons = [], []
        for raw in body.split("\n"):
            line = raw.strip()
            if not line or line.startswith("--"):
                continue
            if line.upper().startswith(("CONSTRAINT", "FOREIGN KEY", "PRIMARY KEY",
                                        "UNIQUE", "CHECK", "REFERENCES", "EXCLUDE")):
                if line.upper().startswith("CONSTRAINT"):
                    cons.append(re.sub(r"\s+", " ", line).rstrip(","))
                continue
            cm = re.match(r"(\w+)\s+", line)
            if cm:
                cols.append(cm.group(1))
        out[table] = {"columns": cols, "constraints": cons}
    return out


def cross_check(rel: Rel, ddl: dict, issues: list):
    """把手写的关系模式与真实 DDL 对照, 列不一致就报错。"""
    table = rel.name.split()[0].strip()
    if table not in ddl:
        if not rel.derived:
            issues.append(f"[{rel.name}] DDL 里找不到这张表")
        return
    ddl_cols = [c for c in ddl[table]["columns"]]
    if set(ddl_cols) != set(rel.attrs):
        issues.append(
            f"[{rel.name}] 手写属性与 DDL 不一致:\n"
            f"    DDL 多出 = {sorted(set(ddl_cols) - set(rel.attrs))}\n"
            f"    手写多出 = {sorted(set(rel.attrs) - set(ddl_cols))}"
        )


# ============================================================
# 4. 体检
# ============================================================


def fmt_set(s):
    return "{" + ",".join(sorted(s)) + "}"


def audit_one(rel: Rel):
    A = frozenset(rel.attrs)
    fds = [(frozenset(l), frozenset(r)) for l, r in rel.fds]
    keys = all_candidate_keys(A, fds)
    prime = set()
    for k in keys:
        prime |= set(k)

    rows = []
    for lhs, rhs in fds:
        trivial = rhs <= lhs
        superkey = is_superkey(lhs, A, fds)
        if trivial:
            verdict = "平凡"
        elif superkey:
            verdict = "BCNF-OK(左边是超键)"
        else:
            # 3NF 逃生口:右边全在主属性上则 3NF 放行, 但 BCNF 不放行
            if set(rhs) <= prime:
                verdict = "违反 BCNF(3NF 逃生口放行:右边全是主属性)"
            else:
                verdict = "违反 3NF(右边含非主属性) => 必然也违反 BCNF"
        rows.append((lhs, rhs, trivial, superkey, verdict))

    violations = [(l, r, v) for l, r, t, s, v in rows if "违反" in v]
    return A, fds, keys, prime, rows, violations


def bcnf_decompose(rel: Rel, violations):
    """BCNF 分解算法:把违反项 X->Y 拆成 (X∪Y) 与 (R−Y)。"""
    if not violations:
        return None
    lhs, rhs, _ = violations[0]
    r1 = set(lhs) | set(rhs)
    r2 = set(rel.attrs) - set(rhs)
    return (sorted(r1), sorted(r2))


def project_fds_fast(sub_attrs, fds):
    """把 FD 集合"投影"到子模式上 —— 快速近似版(只在左/右都落在子模式内时保留)。
    注意: 这不是完备投影, 会漏掉"由原 F 推导出来的"新 FD, 只用于展示。"""
    sub = set(sub_attrs)
    out = []
    for lhs, rhs in fds:
        l = set(lhs) & sub
        r = set(rhs) & sub
        if l and r and not r <= l:
            out.append((frozenset(l), frozenset(r)))
    return out


def project_fds(sub_attrs, fds):
    """FD 集合在子模式上的**完备**投影 Fi:
       对子模式内的每个属性子集 X, 取 X+ ∩ Ri, 所得 X -> (X+∩Ri − X) 即为完整 Fi。
    完备性是"保持依赖"判定的前提 —— 漏掉推导出的 FD 会误判为丢依赖。"""
    sub = set(sub_attrs)
    out = []
    attrs = sorted(sub)
    for size in range(1, len(attrs) + 1):
        for combo in itertools.combinations(attrs, size):
            x = frozenset(combo)
            r = set(closure(x, fds)) & sub - set(x)
            if r:
                out.append((x, frozenset(r)))
    return out


def first_violation(sub_attrs, fds):
    for lhs, rhs in fds:
        if not is_superkey(lhs, set(sub_attrs), fds):
            return (lhs, rhs)
    return None


def bcnf_decompose_all(sub_attrs, fds, trace=None):
    """完整 BCNF 分解:递归拆到每个子模式的 FD 左边都是超键。
    trace 记录 (当前子模式, 依据的 lhs, 依据的 rhs, 拆出的 R1, 拆出的 R2)。"""
    if trace is None:
        trace = []
    v = first_violation(sub_attrs, fds)
    if v is None:
        return [set(sub_attrs)], trace
    lhs, rhs = v
    r1 = set(lhs) | set(rhs)
    r2 = set(sub_attrs) - set(rhs)
    trace.append((set(sub_attrs), lhs, rhs, r1, r2))
    out = []
    for sub in (r1, r2):
        got, _ = bcnf_decompose_all(sub, project_fds(sub, fds), trace)
        out.extend(got)
    return out, trace


def lossless_check(r1, r2, fds, base_attrs):
    """二元分解无损判据:
       R1∩R2 -> (R1−R2)  或  R1∩R2 -> (R2−R1)  至少一个成立(注意:是被原 F 蕴含!)
    """
    inter = set(r1) & set(r2)
    d1 = set(r1) - set(r2)
    d2 = set(r2) - set(r1)
    if not inter:
        cover = set(r1) | set(r2) == set(base_attrs)
        return cover, f"R1∩R2 = 空集; 属性并集覆盖原关系 = {cover}"
    ok1 = d1 and fd_implied(inter, d1, fds)
    ok2 = d2 and fd_implied(inter, d2, fds)
    reason = []
    if ok1:
        reason.append(f"{fmt_set(inter)} -> {fmt_set(d1)} 成立(被 F 蕴含)")
    if ok2:
        reason.append(f"{fmt_set(inter)} -> {fmt_set(d2)} 成立(被 F 蕴含)")
    if not reason:
        reason.append(f"{fmt_set(inter)} 既不确定 (R1-R2) 也不确定 (R2-R1) -> 有损!")
    reason.append(f"闭包 {fmt_set(inter)}+ = {fmt_set(closure(inter, fds))}")
    return bool(ok1 or ok2), "; ".join(reason)


def dependency_preserving(r1, r2, fds):
    """原 FD 是否被两个子模式的**完备投影** FD 集合共同蕴含。"""
    proj = project_fds(r1, fds) + project_fds(r2, fds)
    lost = [(l, r) for l, r in fds if not fd_implied(l, r, proj)]
    return proj, lost


# ============================================================
# 5. 报告
# ============================================================


def main():
    # Windows 控制台缺省 GBK, 报告里有 ∩ ∪ -> 等符号, 强制 UTF-8 输出
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

    ap = argparse.ArgumentParser()
    ap.add_argument("--ddl", default=str(AIRLINE_DDL))
    ap.add_argument("--bookstore-ddl", default=str(ONLINE_BOOKSTORE_DDL))
    ap.add_argument("--outdir", default=str(ROOT / "docs"))
    args = ap.parse_args()

    ddl_path = Path(args.ddl)
    ddl = parse_ddl(ddl_path)

    lines = []
    w = lines.append

    w("=" * 78)
    w("BCNF 体检报告 —— 对象:你自己交付的真实表结构")
    w(f"DDL 来源 : {ddl_path.relative_to(ROOT)}")
    w(f"DDL 表数 : {len(ddl)}  ->  {', '.join(sorted(ddl))}")
    w("FD 来源  : DDL 主码/唯一约束 + 语义追加(在脚本 SCHEMA 里显式标注)")
    w("=" * 78)
    w("")

    # --- 0. 对象核对 ---
    issues = []
    for rel in SCHEMA:
        cross_check(rel, ddl, issues)
    w("【0】体检对象核对(手写关系模式 vs 磁盘上的 DDL)")
    if issues:
        for i in issues:
            w("  ! " + i)
    else:
        for rel in SCHEMA:
            table = rel.name.split()[0]
            if rel.derived:
                w(f"  - {rel.name:<46} 派生对照,跳过核对")
            else:
                w(f"  - {rel.name:<20} 列与 DDL 一致 ({len(rel.attrs)} 列)")
    w("")

    # --- 1. 逐关系体检 ---
    w("【1】逐关系体检(属性闭包 -> 候选键 -> 范式判定)")
    w("")
    summary = []
    for rel in SCHEMA:
        A, fds, keys, prime, rows, violations = audit_one(rel)
        w(f"── {rel.name}")
        w(f"   属性({len(rel.attrs)}): {', '.join(rel.attrs)}")
        if rel.note:
            w(f"   说明: {rel.note}")
        if rel.declared_keys:
            w(f"   DDL 声明的键: {' ; '.join(rel.declared_keys)}")
        w(f"   候选键({len(keys)}): " + " | ".join(fmt_set(k) for k in keys))
        w(f"   主属性: {fmt_set(prime)}")
        w("   FD 判定:")
        for lhs, rhs, triv, sk, verdict in rows:
            mark = "  " if ("OK" in verdict and "VIOLATION" not in verdict) else "!!"
            w(f"   {mark} {fmt_set(lhs)} -> {fmt_set(rhs)}")
            w(f"      左边闭包 = {fmt_set(closure(lhs, fds))}")
            w(f"      结论: {verdict}")
        if violations:
            dec = bcnf_decompose(rel, violations)
            if dec:
                r1, r2 = dec
                ok, why = lossless_check(r1, r2, fds, rel.attrs)
                proj, lost = dependency_preserving(r1, r2, fds)
                w(f"   BCNF 分解(对 {fmt_set(violations[0][0])} -> {fmt_set(violations[0][1])}):")
                w(f"     R1 = {fmt_set(r1)}")
                w(f"     R2 = {fmt_set(r2)}")
                w(f"     无损连接: {'是' if ok else '否'}  ({why})")
                w(f"     保持依赖: {'是' if not lost else '否'}"
                  + (f"  丢失 {[(fmt_set(l), fmt_set(r)) for l, r in lost]}" if lost else ""))
            w(f"   判定: 违反 3NF 的 FD 数 = {len([1 for v in violations if '违反 3NF' in v[2]])}"
              f" ; 3NF 放行但违反 BCNF 的 FD 数 = {len([1 for v in violations if '违反 BCNF' in v[2]])}")
        else:
            w("   判定: 所有非平凡 FD 的左边都是超键 -> **满足 BCNF**")
        summary.append((rel.name, len(keys), len(violations)))
        w("")

    # --- 2. FD 集合等价性演示(证明两条候选键互相蕴含) ---
    w("【2】FD 集合闭包演示:候选键之间互相蕴含(Armstrong 公理算给你看)")
    w("")
    for rel in SCHEMA:
        if len(rel.fds) >= 2:
            fds = [(frozenset(l), frozenset(r)) for l, r in rel.fds]
            for lhs, rhs in fds:
                cl = closure(lhs, fds)
                if is_superkey(lhs, rel.attrs, fds):
                    w(f"   [{rel.name}] {fmt_set(lhs)}+ = {fmt_set(cl)}  (= 全属性 -> 是超键)")
    w("")

    # --- 3. 完全分解推演:课堂上那个"3NF 放行 / BCNF 拆"的经典例 ---
    w("【3】完全 BCNF 分解推演(3NF 与 BCNF 分歧的两个经典例, 结论由程序算, 不靠背)")
    w("")

    def demo(title, attrs, fds):
        w(f"   ● {title}")
        w(f"     属性: {fmt_set(attrs)}")
        w(f"     F = {{ " + " , ".join(f"{fmt_set(l)}->{fmt_set(r)}" for l, r in fds) + " }")
        keys = all_candidate_keys(attrs, fds)
        prime = set()
        for k in keys:
            prime |= set(k)
        w(f"     候选键: " + " | ".join(fmt_set(k) for k in keys)
          + f"     主属性: {fmt_set(prime)}")
        for lhs, rhs in fds:
            sk = is_superkey(lhs, attrs, fds)
            verdict = ("左边是超键 -> BCNF 直接放行" if sk else
                       ("右边全是主属性 -> 3NF 放行, BCNF 不放行" if set(rhs) <= prime
                        else "右边含非主属性 -> 3NF 也违反"))
            w(f"     {fmt_set(lhs)}->{fmt_set(rhs)} : {fmt_set(lhs)}+ = {fmt_set(closure(lhs, fds))}"
              f"  =>  {verdict}")
        subs, trace = bcnf_decompose_all(attrs, fds)
        for cur, lhs, rhs, r1, r2 in trace:
            w(f"     拆 {fmt_set(cur)}  依据 {fmt_set(lhs)}->{fmt_set(rhs)}"
              f"  =>  {fmt_set(r1)}  +  {fmt_set(r2)}")
        w(f"     分解结果: " + "  |  ".join(fmt_set(s) for s in subs))
        if len(subs) == 2:
            ok, why = lossless_check(sorted(subs[0]), sorted(subs[1]), fds, attrs)
            w(f"     无损连接: {'是' if ok else '否'}  ({why})")
            proj, lost = dependency_preserving(sorted(subs[0]), sorted(subs[1]), fds)
            w(f"     保持依赖: {'是' if not lost else '否'}"
              + ("" if not lost
                 else "   丢失 " + ", ".join(f"{fmt_set(l)}->{fmt_set(r)}" for l, r in lost)))
        w("")

    demo("例 A(3NF 放行、BCNF 拆, 且拆完不丢依赖)",
         {"S", "T", "C"},
         [(frozenset(["S", "T"]), frozenset(["C"])),
          (frozenset(["T"]), frozenset(["C"]))])

    demo("例 B(教科书反例: BCNF 分解会丢依赖)",
         {"S", "T", "C"},
         [(frozenset(["S", "T"]), frozenset(["C"])),
          (frozenset(["C"]), frozenset(["T"]))])
    w("   ★ 判读方法: 例 A 与例 B 的唯一区别是那条非键 FD 的右边是不是主属性。")
    w("     同一个 R(S,T,C), 换一种语义就成了'能无损又保依赖'或'只能二选一'。")
    w("")

    # --- 4. 汇总 ---
    w("【4】汇总表")
    w(f"   {'关系':<46}{'候选键数':>8}{'违反 FD 数':>12}")
    for name, nk, nv in summary:
        w(f"   {name:<46}{nk:>8}{nv:>12}")
    total_v = sum(v for _, _, v in summary)
    w("")
    w(f"   结论: {len(summary)} 个关系, 违反 BCNF 的 FD 共 {total_v} 条。")
    w("")

    report = "\n".join(lines)
    print(report)

    outdir = Path(args.outdir).resolve()
    outdir.mkdir(parents=True, exist_ok=True)
    out = outdir / "bcnf-audit-raw.txt"
    out.write_text(report, encoding="utf-8")
    try:
        shown = out.relative_to(ROOT)
    except ValueError:
        shown = out
    print(f"[已写出] {shown}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
