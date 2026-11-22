# Phân tích sau khi chạy, cho phần trả lời nhận xét báo cáo tuần 5 (docs/ket_qua.md, mục "Bổ sung theo nhận xét").
# Chỉ đọc CSV trong thuc_nghiem/ket_qua/, không ghi gì. Chạy từ thư mục gốc dự án:
#     python thuc_nghiem/phan_tich/bo_sung_tuan_6.py
#
# 1. Kiểm lượt bổ sung lặp lại đúng lượt báo cáo (mọi ô của B1, P; phép P − B1).
# 2. Hiệu chỉnh kiểm định bội Holm–Bonferroni (D100): họ 1 = 8 phép của lượt báo cáo; họ 2 = các phép gộp 3 seed
#    của lượt bổ sung.
# 3. Kiểm định sau cho cấu hình đã chọn (P_sf_tran3 − P, P_w_lich − P; D101), từ per_user.csv của lượt báo cáo.
#    Wilcoxon theo đúng công thức D62 (có tự kiểm: tính lại P − B1 phải ra đúng z, p của kiem_dinh.csv).
#    Bootstrap dùng random của Python nên khoảng tin cậy có thể lệch chữ số cuối so với cách Java tính.
import csv
import math
import random
import sys

BASE = 'thuc_nghiem/ket_qua/'
BAO_CAO = BASE + '2026-11-14_bao-cao/'
BO_SUNG = BASE + '2026-11-22_bo-sung/'


def read(path):
    with open(path, encoding='utf-8') as f:
        return list(csv.DictReader(f))


def holm(ps):
    """p đã hiệu chỉnh Holm–Bonferroni, cùng thứ tự với ps."""
    order = sorted(range(len(ps)), key=lambda i: ps[i])
    adjusted = [0.0] * len(ps)
    running = 0.0
    for rank, i in enumerate(order):
        running = max(running, min(1.0, (len(ps) - rank) * ps[i]))
        adjusted[i] = running
    return adjusted


def wilcoxon(diffs):
    """Wilcoxon theo cặp như D62: bỏ hiệu |d| < 1e-9, hạng trung bình khi bằng nhau, hiệu chỉnh phương sai, không hiệu chỉnh liên tục."""
    d = [x for x in diffs if abs(x) >= 1e-9]
    n = len(d)
    order = sorted(range(n), key=lambda i: abs(d[i]))
    ranks = [0.0] * n
    ties = 0.0
    i = 0
    while i < n:
        j = i
        while j + 1 < n and abs(abs(d[order[j + 1]]) - abs(d[order[i]])) < 1e-9:
            j += 1
        for k in range(i, j + 1):
            ranks[order[k]] = (i + j) / 2 + 1
        t = j - i + 1
        ties += t ** 3 - t
        i = j + 1
    w_plus = sum(r for r, x in zip(ranks, d) if x > 0)
    z = (w_plus - n * (n + 1) / 4) / math.sqrt(n * (n + 1) * (2 * n + 1) / 24 - ties / 48)
    return n, w_plus, z, math.erfc(abs(z) / math.sqrt(2))


def bootstrap_ci(diffs, times=2000, seed=3_000_000):
    rnd = random.Random(seed)
    n = len(diffs)
    means = sorted(sum(diffs[rnd.randrange(n)] for _ in range(n)) / n for _ in range(times))
    return means[math.ceil(0.025 * times) - 1], means[math.ceil(0.975 * times) - 1]


def paired(per_user, a, b, metric, seed=None):
    rows = [r for r in per_user if r['n'] == '500' and r['muc_nhieu'] == 'mac_dinh' and (seed is None or r['seed'] == seed)]
    A = {(r['seed'], r['user_id']): r for r in rows if r['phuong_an'] == a}
    B = {(r['seed'], r['user_id']): r for r in rows if r['phuong_an'] == b}
    diffs = [float(A[k][metric]) - float(B[k][metric]) for k in A if int(A[k]['so_muc_nhan_ly_tuong']) >= 2]
    return diffs


def main():
    ok = True
    m_bc, m_bs = read(BAO_CAO + 'metrics.csv'), read(BO_SUNG + 'metrics.csv')
    k_bc, k_bs = read(BAO_CAO + 'kiem_dinh.csv'), read(BO_SUNG + 'kiem_dinh.csv')

    print('1. Lượt bổ sung lặp lại lượt báo cáo')
    index = {(r['seed'], r['n'], r['muc_nhieu'], r['phuong_an'], r['chi_so']): r['gia_tri'] for r in m_bc}
    same = [index.get((r['seed'], r['n'], r['muc_nhieu'], r['phuong_an'], r['chi_so'])) == r['gia_tri']
            for r in m_bs if r['phuong_an'] in ('B1', 'P')
            and (r['seed'], r['n'], r['muc_nhieu'], r['phuong_an'], r['chi_so']) in index]
    print(f'   {sum(same)}/{len(same)} ô của B1, P trùng')
    ok &= all(same)
    tests_bc = {(r['n'], r['phuong_an'], r['so_voi'], r['chi_so'], r['seeds']): r for r in k_bc}
    for r in k_bs:
        key = (r['n'], r['phuong_an'], r['so_voi'], r['chi_so'], r['seeds'])
        if key in tests_bc:
            print(f'   phép {key[1]} − {key[2]} {key[3]} gộp seed {key[4]}:', 'giống hệt' if tests_bc[key] == r else 'KHÁC')
            ok &= tests_bc[key] == r

    print('\n2. Holm–Bonferroni')
    print('   Họ 1: 8 phép của lượt báo cáo')
    for r, p_holm in zip(k_bc, holm([float(r['p']) for r in k_bc])):
        print(f"     P − {r['so_voi']:14s} {r['chi_so']:28s} p = {float(r['p']):.4f}  p_Holm = {p_holm:.4f}")
    family2 = [r for r in k_bs if ';' in r['seeds'] and r['p']]
    print(f'   Họ 2: {len(family2)} phép gộp 3 seed của lượt bổ sung')
    for r, p_holm in zip(family2, holm([float(r['p']) for r in family2])):
        print(f"     N = {r['n']:4s} {r['phuong_an']} − {r['so_voi']} {r['chi_so']}: p = {float(r['p']):.4g}  p_Holm = {p_holm:.4g}")

    print('\n3. Cấu hình đã chọn so với P (N = 500, nhiễu mặc định, người thuộc bản (2))')
    per_user = read(BAO_CAO + 'per_user.csv')
    check = wilcoxon(paired(per_user, 'P', 'B1', 'ndcg_at_5'))
    java = [r for r in k_bc if r['so_voi'] == 'B1' and r['chi_so'] == 'ndcg_at_5'][0]
    # W+ và z phải trùng. p của Java tính Φ bằng xấp xỉ erf 7.1.26 (sai số dưới 1.5e-7, D62), Python dùng erfc
    # chính xác, nên p chỉ cần lệch dưới 2 × 1.5e-7.
    match = check[1] == float(java['w_cong']) and abs(check[2] - float(java['z'])) < 1e-9 and abs(check[3] - float(java['p'])) < 3e-7
    print(f'   tự kiểm Wilcoxon với P − B1 của Java: {"khớp" if match else "LỆCH"}')
    ok &= match
    for a in ('P_sf_tran3', 'P_w_lich'):
        for metric in ('ndcg_at_5', 'precision_at_5_nghiem_ngat'):
            d = paired(per_user, a, 'P', metric)
            n, w, z, p = wilcoxon(d)
            lo, hi = bootstrap_ci(d)
            per_seed = []
            for seed in ('3', '4', '5'):
                ds = paired(per_user, a, 'P', metric, seed)
                per_seed.append(f'{sum(ds) / len(ds):+.4f} (p {wilcoxon(ds)[3]:.4f})')
            print(f'   {a} − P [{metric}]: {len(d)} cặp, hiệu {sum(d) / len(d):+.4f}, KTC 95% [{lo:+.4f}, {hi:+.4f}], '
                  f'n_W {n}, W+ {w}, z {z:.2f}, p {p:.4g}; từng seed 3/4/5: {", ".join(per_seed)}')

    sys.exit(0 if ok else 1)


if __name__ == '__main__':
    main()
