package com.skillexchange.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.skillexchange.matching.SkillRelation;

/**
 * Danh mục kỹ năng cho dữ liệu giả lập: bản chép của V2__seed_skills.sql (D63).
 *
 * Module evaluation không đọc DB (D71), nên danh mục là hằng số trong code.
 * Mã kỹ năng là 1..26 theo thứ tự trong V2, không cần khớp id của DB.
 * SkillCatalogTest đọc lại chính file V2 để chắc bản chép này không lệch.
 */
public final class SkillCatalog {

    /** Một kỹ năng: mã 1..26, tên, tên nhóm. */
    public record Skill(long id, String name, String category) {
    }

    /** Bố cục giống khối VALUES trong V2 để đối chiếu bằng mắt. */
    public static final SkillCatalog V2 = new SkillCatalog(
            List.of("Lập trình", "Dữ liệu", "Thiết kế", "Ngoại ngữ", "Âm nhạc", "Kỹ năng mềm"),
            new String[][] {
                    {"Lập trình", "Java"},
                    {"Lập trình", "Kotlin"},
                    {"Lập trình", "Python"},
                    {"Lập trình", "JavaScript"},
                    {"Lập trình", "TypeScript"},
                    {"Lập trình", "C++"},
                    {"Dữ liệu", "SQL"},
                    {"Dữ liệu", "MySQL"},
                    {"Dữ liệu", "Excel"},
                    {"Dữ liệu", "Power BI"},
                    {"Thiết kế", "Figma"},
                    {"Thiết kế", "Photoshop"},
                    {"Thiết kế", "Illustrator"},
                    {"Thiết kế", "Canva"},
                    {"Ngoại ngữ", "Tiếng Anh"},
                    {"Ngoại ngữ", "Tiếng Nhật"},
                    {"Ngoại ngữ", "Tiếng Hàn"},
                    {"Ngoại ngữ", "Tiếng Trung"},
                    {"Âm nhạc", "Guitar"},
                    {"Âm nhạc", "Ukulele"},
                    {"Âm nhạc", "Piano"},
                    {"Âm nhạc", "Thanh nhạc"},
                    {"Kỹ năng mềm", "Thuyết trình"},
                    {"Kỹ năng mềm", "Viết CV"},
                    {"Kỹ năng mềm", "Quản lý thời gian"},
                    {"Kỹ năng mềm", "Làm việc nhóm"},
            },
            new String[][] {
                    {"Java", "Kotlin"},
                    {"JavaScript", "TypeScript"},
                    {"SQL", "MySQL"},
                    {"Excel", "Power BI"},
                    {"Photoshop", "Illustrator"},
                    {"Figma", "Canva"},
                    {"Guitar", "Ukulele"},
                    {"Tiếng Nhật", "Tiếng Trung"},
                    {"Tiếng Nhật", "Tiếng Hàn"},
            });

    private final List<String> categories;
    private final List<Skill> skills;
    private final List<SkillRelation> relations;
    // Chỉ dùng để tra theo tên, không bao giờ duyệt, nên thứ tự của HashMap không ảnh hưởng tái lập (D64).
    private final Map<String, Long> idByName = new HashMap<>();

    private SkillCatalog(List<String> categories, String[][] skillRows, String[][] relationRows) {
        this.categories = List.copyOf(categories);

        List<Skill> skillList = new ArrayList<>();
        for (String[] row : skillRows) {
            String category = row[0];
            String name = row[1];
            if (!this.categories.contains(category)) {
                throw new IllegalArgumentException("Nhóm không có trong danh mục: " + category);
            }
            long id = skillList.size() + 1; // mã theo thứ tự dòng, bắt đầu từ 1
            if (idByName.put(name, id) != null) {
                throw new IllegalArgumentException("Tên kỹ năng bị lặp: " + name);
            }
            skillList.add(new Skill(id, name, category));
        }
        this.skills = List.copyOf(skillList);

        List<SkillRelation> relationList = new ArrayList<>();
        for (String[] row : relationRows) {
            relationList.add(new SkillRelation(idOf(row[0]), idOf(row[1])));
        }
        this.relations = List.copyOf(relationList);
    }

    /** Tên 6 nhóm, theo thứ tự trong V2. */
    public List<String> categories() {
        return categories;
    }

    /** 26 kỹ năng, theo mã tăng dần (cũng là thứ tự trong V2). */
    public List<Skill> skills() {
        return skills;
    }

    /** 9 cặp liên quan, đưa vào new SkillSimilarity(...) của lõi matching. */
    public List<SkillRelation> relations() {
        return relations;
    }

    public String nameOf(long skillId) {
        if (skillId < 1 || skillId > skills.size()) {
            throw new IllegalArgumentException("Không có kỹ năng mã " + skillId);
        }
        return skills.get((int) skillId - 1).name();
    }

    public long idOf(String name) {
        Long id = idByName.get(name);
        if (id == null) {
            throw new IllegalArgumentException("Không có kỹ năng tên " + name);
        }
        return id;
    }
}
