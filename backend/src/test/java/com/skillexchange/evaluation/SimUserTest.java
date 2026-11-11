package com.skillexchange.evaluation;

import static com.skillexchange.user.Period.EVENING;
import static com.skillexchange.user.Period.MORNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.skillexchange.matching.LearnItem;
import com.skillexchange.matching.MatchProfile;
import com.skillexchange.matching.Slot;
import com.skillexchange.matching.TeachItem;
import com.skillexchange.user.LearningMode;
import com.skillexchange.user.Role;
import com.skillexchange.user.UserStatus;

/**
 * Người dùng giả lập hai lớp (thật / tự khai): TC210 (spec 5.1, D53) và các ràng buộc của D53, D63, D64.
 * Mã kỹ năng ghi bằng số theo thứ tự trong V2__seed_skills.sql (chốt ở kế hoạch bước 1), không tra qua SkillCatalog,
 * để test này không phụ thuộc phần tra cứu đang được kiểm ở SkillCatalogTest.
 */
class SimUserTest {

    private static final long JAVA = 1;
    private static final long PYTHON = 3;
    private static final long SQL = 7;

    private static final Slot MON_MORNING = new Slot(1, MORNING);
    private static final Slot WED_EVENING = new Slot(3, EVENING);
    private static final Slot FRI_EVENING = new Slot(5, EVENING);

    @Test
    @DisplayName("TC210 MatchProfile mang trình độ và lịch tự khai, ACTIVE, STUDENT, reputation 0.5")
    void tc210_matchProfileUsesDeclaredValues() {
        SimUser user = new SimUser(42,
                List.of(new SimTeach(JAVA, 2, 3)),                       // thật 2, tự khai 3
                List.of(new SimLearn(SQL, 2), new SimLearn(PYTHON, null)),
                List.of(MON_MORNING, WED_EVENING),                       // lịch thật
                List.of(WED_EVENING, FRI_EVENING),                       // tự khai: sót Sáng T2, thêm Tối T6
                LearningMode.ONLINE);

        MatchProfile profile = user.toMatchProfile(SkillCatalog.V2);

        assertThat(profile.userId()).isEqualTo(42);
        assertThat(profile.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(profile.role()).isEqualTo(Role.STUDENT);
        assertThat(profile.reputation()).isEqualTo(0.5);
        assertThat(profile.teach()).containsExactly(new TeachItem(JAVA, "Java", 3));
        assertThat(profile.learn()).containsExactly(
                new LearnItem(SQL, "SQL", 2),
                new LearnItem(PYTHON, "Python", null));
        assertThat(profile.slots()).containsExactlyInAnyOrder(WED_EVENING, FRI_EVENING);
        assertThat(profile.mode()).isEqualTo(LearningMode.ONLINE);
    }

    @Test
    @DisplayName("D53 người chưa khai lịch và hình thức: hai lớp lịch rỗng, MatchProfile có lịch rỗng và mode null")
    void d53_undeclaredScheduleAndMode() {
        SimUser user = new SimUser(7,
                List.of(new SimTeach(JAVA, 3, 3)),
                List.of(new SimLearn(SQL, null)),
                List.of(),
                List.of(),
                null);

        MatchProfile profile = user.toMatchProfile(SkillCatalog.V2);

        assertThat(profile.slots()).isEmpty();
        assertThat(profile.mode()).isNull();
    }

    @Test
    @DisplayName("D53 lịch chỉ có ở một lớp (thật có mà tự khai rỗng, hoặc ngược lại) bị từ chối")
    void d53_scheduleDeclaredInBothLayersOrNeither() {
        List<SimTeach> teach = List.of(new SimTeach(JAVA, 3, 3));
        List<SimLearn> learn = List.of(new SimLearn(SQL, 2));

        assertThatThrownBy(() -> new SimUser(1, teach, learn, List.of(MON_MORNING), List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SimUser(1, teach, learn, List.of(), List.of(MON_MORNING), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("D63 trình độ thật, trình độ tự khai và mục tiêu ngoài 1–4 bị từ chối; mục tiêu null được nhận")
    void d63_levelsOutsideOneToFourRejected() {
        assertThatThrownBy(() -> new SimTeach(JAVA, 0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SimTeach(JAVA, 4, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SimLearn(SQL, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SimLearn(SQL, 5)).isInstanceOf(IllegalArgumentException.class);

        assertThat(new SimTeach(JAVA, 1, 4).declaredLevel()).isEqualTo(4);
        assertThat(new SimLearn(SQL, null).target()).isNull();
    }

    @Test
    @DisplayName("D64 các danh sách giữ đúng thứ tự đưa vào và không đổi khi danh sách gốc bị sửa")
    void d64_listsKeepOrderAndAreCopied() {
        List<Slot> trueSlots = new ArrayList<>(List.of(WED_EVENING, MON_MORNING, FRI_EVENING));
        SimUser user = new SimUser(1,
                List.of(new SimTeach(JAVA, 3, 3)),
                List.of(new SimLearn(SQL, 2)),
                trueSlots,
                List.of(FRI_EVENING),
                null);

        trueSlots.clear();

        assertThat(user.trueSlots()).containsExactly(WED_EVENING, MON_MORNING, FRI_EVENING);
    }
}
