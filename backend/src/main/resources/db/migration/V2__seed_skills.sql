-- =====================================================================
-- V2__seed_skills.sql – Danh mục kỹ năng ban đầu (D3)
-- Nguồn: docs/tuan_3/ke_hoach_tuan_3.md, mục 1 (danh sách đã duyệt).
-- Mong đợi: 6 nhóm, 26 kỹ năng, 9 quan hệ. Sai số thì RAISE EXCEPTION.
-- =====================================================================

-- ---------- Nhóm kỹ năng ----------
INSERT INTO skill_categories (name) VALUES
    ('Lập trình'),
    ('Dữ liệu'),
    ('Thiết kế'),
    ('Ngoại ngữ'),
    ('Âm nhạc'),
    ('Kỹ năng mềm');

-- ---------- Kỹ năng ----------
-- Tra category_id theo tên nhóm. Gõ sai tên nhóm thì JOIN không khớp,
-- INSERT chèn thiếu dòng và bước kiểm tra cuối file sẽ báo lỗi.
INSERT INTO skills (category_id, name)
SELECT c.id, s.skill_name
FROM (VALUES
    ('Lập trình',   'Java'),
    ('Lập trình',   'Kotlin'),
    ('Lập trình',   'Python'),
    ('Lập trình',   'JavaScript'),
    ('Lập trình',   'TypeScript'),
    ('Lập trình',   'C++'),
    ('Dữ liệu',     'SQL'),
    ('Dữ liệu',     'MySQL'),
    ('Dữ liệu',     'Excel'),
    ('Dữ liệu',     'Power BI'),
    ('Thiết kế',    'Figma'),
    ('Thiết kế',    'Photoshop'),
    ('Thiết kế',    'Illustrator'),
    ('Thiết kế',    'Canva'),
    ('Ngoại ngữ',   'Tiếng Anh'),
    ('Ngoại ngữ',   'Tiếng Nhật'),
    ('Ngoại ngữ',   'Tiếng Hàn'),
    ('Ngoại ngữ',   'Tiếng Trung'),
    ('Âm nhạc',     'Guitar'),
    ('Âm nhạc',     'Ukulele'),
    ('Âm nhạc',     'Piano'),
    ('Âm nhạc',     'Thanh nhạc'),
    ('Kỹ năng mềm', 'Thuyết trình'),
    ('Kỹ năng mềm', 'Viết CV'),
    ('Kỹ năng mềm', 'Quản lý thời gian'),
    ('Kỹ năng mềm', 'Làm việc nhóm')
) AS s(category_name, skill_name)
JOIN skill_categories c ON c.name = s.category_name;

-- ---------- Kỹ năng liên quan ----------
-- Tra id theo tên; chỉ nhận cặp cùng nhóm (a.category_id = b.category_id).
-- LEAST/GREATEST để thỏa CHECK (skill_a_id < skill_b_id) dù viết cặp theo thứ tự nào.
INSERT INTO skill_relations (skill_a_id, skill_b_id)
SELECT LEAST(a.id, b.id), GREATEST(a.id, b.id)
FROM (VALUES
    ('Java',        'Kotlin'),
    ('JavaScript',  'TypeScript'),
    ('SQL',         'MySQL'),
    ('Excel',       'Power BI'),
    ('Photoshop',   'Illustrator'),
    ('Figma',       'Canva'),
    ('Guitar',      'Ukulele'),
    ('Tiếng Nhật',  'Tiếng Trung'),
    ('Tiếng Nhật',  'Tiếng Hàn')
) AS r(name_a, name_b)
JOIN skills a ON a.name = r.name_a
JOIN skills b ON b.name = r.name_b
            AND b.category_id = a.category_id;

-- ---------- Kiểm tra số dòng ----------
-- INSERT ... SELECT không báo lỗi khi JOIN không khớp (chỉ chèn 0 dòng),
-- nên đếm lại và dừng Flyway nếu sai số mong đợi.
DO $$
DECLARE
    category_count INTEGER;
    skill_count    INTEGER;
    relation_count INTEGER;
BEGIN
    SELECT count(*) INTO category_count FROM skill_categories;
    SELECT count(*) INTO skill_count    FROM skills;
    SELECT count(*) INTO relation_count FROM skill_relations;

    IF category_count <> 6 OR skill_count <> 26 OR relation_count <> 9 THEN
        RAISE EXCEPTION 'V2 seed sai số dòng: % nhóm (mong đợi 6), % kỹ năng (mong đợi 26), % quan hệ (mong đợi 9)',
            category_count, skill_count, relation_count;
    END IF;
END
$$;
