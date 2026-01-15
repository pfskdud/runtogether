package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.*;
import runtogether.server.dto.GroupDto;
import runtogether.server.repository.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final RunningGroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final RunRecordRepository runRecordRepository;

    // ★ [추가됨] Controller에서 사용하는 단순 조회 헬퍼 메소드
    @Transactional(readOnly = true)
    public RunningGroup getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
    }

    // 1. 그룹 생성
    @Transactional
    public Long createGroup(String userEmail, GroupDto.CreateRequest request) {
        System.out.println("====== [1] createGroup 시작! 요청 이메일: " + userEmail);
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        System.out.println("====== [2] 방장 찾기 성공: " + owner.getNickname());

        RunningGroup group = new RunningGroup(
                request.getGroupName(),
                request.getDescription(),
                request.isSecret(),
                request.isSearchable(),
                request.getMaxPeople(),
                request.getTags(),
                owner
        );
        System.out.println("====== [3] 그룹 객체 생성 완료");
        RunningGroup savedGroup = groupRepository.save(group);
        System.out.println("====== [4] 그룹 DB 저장 완료 ID: " + savedGroup.getId());

        userGroupRepository.save(new UserGroup(owner, savedGroup));
        System.out.println("====== [5] 방장 멤버 등록 완료");

        if (request.getCourseId() != null) {
            System.out.println("====== [6] 코스 연결 시도. 코스ID: " + request.getCourseId());
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 코스가 존재하지 않습니다."));

            System.out.println("====== [7] 코스 찾기 성공: " + course.getTitle());
            LocalDate start = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
            LocalDate end = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);
            course.updateGroupAndSchedule(savedGroup, start, end);
        }

        return savedGroup.getId();
    }

    // 2. 코스 추가
    @Transactional
    public void addCourse(Long groupId, GroupDto.AddCourseRequest request) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        LocalDate start = LocalDate.parse(request.getStartDate());
        LocalDate end = LocalDate.parse(request.getEndDate());

        Course course = new Course(
                request.getTitle(),
                request.getDistance(),
                request.getExpectedTime(),
                request.getPathData(),
                request.getDescription(),
                start,
                end,
                group
        );
        courseRepository.save(course);
    }

    // 3. 그룹 참여
    @Transactional
    public String joinGroup(String email, Long groupId, String inputCode) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        RunningGroup group = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        if (userGroupRepository.existsByUserAndRunningGroup(user, group)) {
            throw new IllegalArgumentException("이미 가입된 그룹입니다.");
        }

        if (group.isSecret()) {
            if (inputCode == null || !inputCode.equals(group.getAccessCode())) {
                throw new IllegalArgumentException("입장 코드가 올바르지 않습니다.");
            }
        }

        int currentCount = userGroupRepository.countByRunningGroup(group);
        if (group.getMaxPeople() != null && currentCount >= group.getMaxPeople()) {
            throw new IllegalArgumentException("정원이 초과되어 가입할 수 없습니다.");
        }

        userGroupRepository.save(new UserGroup(user, group));
        return "그룹 가입 완료!";
    }

    // ★ [추가] 초대 코드만으로 그룹 찾아서 가입하기
    @Transactional
    public Long joinGroupByAccessCode(String email, String accessCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        // 1. 코드로 그룹 찾기 (Repository에 이 기능이 있어야 함)
        RunningGroup group = groupRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 입장 코드입니다."));

        // 2. 이미 가입했는지 확인
        if (userGroupRepository.existsByUserAndRunningGroup(user, group)) {
            throw new IllegalArgumentException("이미 가입된 그룹입니다.");
        }

        // 3. 인원 제한 확인
        int currentCount = userGroupRepository.countByRunningGroup(group);
        if (group.getMaxPeople() != null && currentCount >= group.getMaxPeople()) {
            throw new IllegalArgumentException("정원이 초과되어 가입할 수 없습니다.");
        }

        // 4. 가입 처리
        userGroupRepository.save(new UserGroup(user, group));

        return group.getId(); // 가입된 그룹 ID 반환
    }

    // 4. 그룹 목록 조회
    @Transactional(readOnly = true)
    public List<GroupDto.Response> getFilteredGroups(String keyword, String status, String type) {
        List<RunningGroup> groups;

        if (keyword != null && !keyword.trim().isEmpty()) {
            groups = groupRepository.findAllByNameContainingOrTagsContaining(keyword, keyword);
        } else {
            groups = groupRepository.findAll();
        }

        return groups.stream()
                .map(group -> {
                    int currentCount = userGroupRepository.countByRunningGroup(group);

                    Long courseId = courseRepository.findByRunningGroup(group)
                            .stream().findFirst().map(Course::getId).orElse(null);

                    return new GroupDto.Response(
                            group.getId(),
                            group.getName(),
                            group.getDescription(),
                            group.isSecret(),
                            group.getOwner().getNickname(),
                            group.getMaxPeople(),
                            group.getTags(),
                            currentCount,
                            courseId
                    );
                })
                .filter(dto -> {
                    if ("public".equals(type) && dto.isSecret()) return false;
                    if ("recruiting".equals(status)) {
                        boolean isFull = dto.getMaxPeople() != null && dto.getCurrentPeople() >= dto.getMaxPeople();
                        if (isFull) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // 5. 그룹 상세 조회
    @Transactional(readOnly = true)
    public GroupDto.DetailResponse getGroupDetail(String email, Long groupId) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        boolean isOwner = group.getOwner().getId().equals(user.getId());

        Course course = courseRepository.findByRunningGroup(group).stream()
                .findFirst()
                .orElse(null);
        Long courseId = (course != null) ? course.getId() : null;

        return new GroupDto.DetailResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.isSecret(),
                group.getAccessCode(),
                isOwner,
                courseId
        );
    }

    // 6. 그룹 수정
    @Transactional
    public void updateGroup(String email, Long groupId, GroupDto.UpdateRequest request) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        if (!group.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("방장만 수정할 수 있습니다.");
        }
        group.updateInfo(request.getGroupName(), request.getDescription());
    }

    // 7. 그룹 삭제
    @Transactional
    public void deleteGroup(String email, Long groupId) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        if (!group.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("방장만 삭제할 수 있습니다.");
        }
        groupRepository.delete(group);
    }

    // 8. 그룹 메인 화면 조회
    @Transactional(readOnly = true)
    public GroupDto.MainResponse getGroupMain(String email, Long groupId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        Course mainCourse = courseRepository.findByRunningGroup(group)
                .stream().findFirst().orElse(null);

        String courseName = (mainCourse != null) ? mainCourse.getTitle() : "등록된 코스 없음";
        Double goalDistance = (mainCourse != null) ? mainCourse.getDistance() : 0.0;

        Long courseId = (mainCourse != null) ? mainCourse.getId() : null;

        String datePeriod = "";
        String dDayString = "D-Day";

        if (mainCourse != null && mainCourse.getStartDate() != null) {
            datePeriod = mainCourse.getStartDate() + " ~ " + mainCourse.getEndDate();
            long days = ChronoUnit.DAYS.between(LocalDate.now(), mainCourse.getStartDate());
            if (days > 0) dDayString = "D-" + days;
            else if (days == 0) dDayString = "D-Day";
            else dDayString = "D+" + Math.abs(days);
        }

        return new GroupDto.MainResponse(
                group.getName(),
                courseName,
                datePeriod,
                dDayString,
                user.getNickname(),
                0.0,
                goalDistance,
                "dummy_profile_url",
                courseId
        );
    }

    // 9. 내 그룹 목록 조회
    @Transactional(readOnly = true)
    public List<GroupDto.Response> getMyGroups(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        List<UserGroup> myUserGroups = userGroupRepository.findAllByUser(user);

        return myUserGroups.stream()
                .map(userGroup -> {
                    RunningGroup group = userGroup.getRunningGroup();
                    int currentCount = userGroupRepository.countByRunningGroup(group);

                    Long courseId = courseRepository.findByRunningGroup(group)
                            .stream().findFirst().map(Course::getId).orElse(null);

                    return new GroupDto.Response(
                            group.getId(),
                            group.getName(),
                            group.getDescription(),
                            group.isSecret(),
                            group.getOwner().getNickname(),
                            group.getMaxPeople(),
                            group.getTags(),
                            currentCount,
                            courseId
                    );
                })
                .collect(Collectors.toList());
    }
}