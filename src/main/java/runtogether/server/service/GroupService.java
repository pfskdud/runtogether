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

    // 헬퍼 메소드
    @Transactional(readOnly = true)
    public RunningGroup getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
    }

    // 1. 그룹 생성 (날짜 + 코스 모두 완벽하게 처리)
    @Transactional
    public Long createGroup(String userEmail, GroupDto.CreateRequest request) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 날짜 변환
        LocalDate start = LocalDate.parse(request.getStartDate(), DateTimeFormatter.ISO_DATE);
        LocalDate end = LocalDate.parse(request.getEndDate(), DateTimeFormatter.ISO_DATE);

        // ★ 날짜를 포함하여 그룹 생성
        RunningGroup group = new RunningGroup(
                request.getGroupName(),
                request.getDescription(),
                request.isSecret(),
                request.isSearchable(),
                request.getMaxPeople(),
                request.getTags(),
                owner,
                start, // 날짜 저장 1
                end    // 날짜 저장 2
        );

        // ★ 코스 연결 (ID가 있으면 연결)
        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 코스가 존재하지 않습니다."));
            group.setCourse(course); // 코스 저장
        }

        RunningGroup savedGroup = groupRepository.save(group);
        userGroupRepository.save(new UserGroup(owner, savedGroup));

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

        int currentCount = userGroupRepository.countByGroupId(group.getId());
        if (group.getMaxPeople() != null && currentCount >= group.getMaxPeople()) {
            throw new IllegalArgumentException("정원이 초과되어 가입할 수 없습니다.");
        }

        userGroupRepository.save(new UserGroup(user, group));
        return "그룹 가입 완료!";
    }

    // 초대 코드로 가입
    @Transactional
    public Long joinGroupByAccessCode(String email, String accessCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        RunningGroup group = groupRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 입장 코드입니다."));

        if (userGroupRepository.existsByUserAndRunningGroup(user, group)) {
            throw new IllegalArgumentException("이미 가입된 그룹입니다.");
        }

        int currentCount = userGroupRepository.countByGroupId(group.getId());
        if (group.getMaxPeople() != null && currentCount >= group.getMaxPeople()) {
            throw new IllegalArgumentException("정원이 초과되어 가입할 수 없습니다.");
        }

        userGroupRepository.save(new UserGroup(user, group));
        return group.getId();
    }

    // 4. 그룹 목록 조회
    @Transactional(readOnly = true)
    public List<GroupDto.Response> getFilteredGroups(String keyword, String status, String type) {
        List<RunningGroup> groups;

        // 주의: Repository 메소드 이름도 findByName... 에서 findByGroupName... 으로 바뀌었을 수 있음
        // 만약 에러나면 Repository도 확인 필요
        if (keyword != null && !keyword.trim().isEmpty()) {
            groups = groupRepository.findAllByGroupNameContainingOrTagsContaining(keyword, keyword);
        } else {
            groups = groupRepository.findAll();
        }

        return groups.stream()
                .map(group -> {
                    int currentCount = userGroupRepository.countByGroupId(group.getId());

                    // ★ getName() -> getGroupName() 수정
                    System.out.println(">>> [DEBUG] 그룹ID: " + group.getId() + ", 이름: " + group.getGroupName());

                    Long courseId = courseRepository.findByRunningGroup(group)
                            .stream().findFirst().map(Course::getId).orElse(null);

                    return new GroupDto.Response(
                            group.getId(),
                            group.getGroupName(), // ★ getName() -> getGroupName()
                            group.getDescription(),
                            group.isSecret(),
                            group.getOwner().getNickname(),
                            group.getMaxPeople(),
                            group.getTags(),
                            currentCount,
                            courseId,
                            false
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

    // 5. 그룹 상세 조회 (코스 이름 + 날짜 조회)
    @Transactional(readOnly = true)
    public GroupDto.DetailResponse getGroupDetail(String email, Long groupId) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        boolean isOwner = group.getOwner().getId().equals(user.getId());

        long dDay = 0;
        if (group.getStartDate() != null) {
            dDay = ChronoUnit.DAYS.between(LocalDate.now(), group.getStartDate());
        }

        // 코스 정보 가져오기
        Course course = group.getCourse();
        Long courseId = (course != null) ? course.getId() : null;

        return new GroupDto.DetailResponse(
                group.getId(),
                group.getGroupName(),
                group.getDescription(),
                group.isSecret(),
                group.getAccessCode(),
                isOwner,
                courseId,
                group.getStartDate(), // DB에서 꺼낸 날짜
                group.getEndDate(),
                dDay
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
        // ★ updateInfo 메소드도 groupName을 받도록 수정되었는지 확인
        group.updateInfo(request.getGroupName(), request.getDescription());
    }

    // 7. 그룹 삭제
    @Transactional
    public void deleteGroup(String email, Long groupId) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        if (!group.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("방장만 삭제할 수 있습니다.");
        }

        try {
            if (runRecordRepository != null) {
                runRecordRepository.deleteByRunningGroup(group);
            }

            List<Course> courses = courseRepository.findByRunningGroup(group);
            for (Course course : courses) {
                course.disconnectGroup();
                courseRepository.save(course);
            }

            userGroupRepository.deleteByRunningGroup(group);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("삭제 중 오류 발생: " + e.getMessage());
        }
        groupRepository.delete(group);
    }

    // 8. 메인 화면 조회
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

        // ★ 그룹 날짜 사용 (코스 날짜 대신 그룹 날짜가 더 정확할 수 있음)
        LocalDate startDate = group.getStartDate();
        LocalDate endDate = group.getEndDate();

        if (startDate != null) {
            datePeriod = startDate + " ~ " + endDate;
            long days = ChronoUnit.DAYS.between(LocalDate.now(), startDate);
            if (days > 0) dDayString = "D-" + days;
            else if (days == 0) dDayString = "D-Day";
            else dDayString = "D+" + Math.abs(days);
        }

        return new GroupDto.MainResponse(
                group.getGroupName(), // ★ getName() -> getGroupName()
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
                    int currentCount = userGroupRepository.countByGroupId(group.getId());

                    Long courseId = courseRepository.findByRunningGroup(group)
                            .stream().findFirst().map(Course::getId).orElse(null);

                    boolean isOwner = group.getOwner().getId().equals(user.getId());

                    return new GroupDto.Response(
                            group.getId(),
                            group.getGroupName(), // ★ getName() -> getGroupName()
                            group.getDescription(),
                            group.isSecret(),
                            group.getOwner().getNickname(),
                            group.getMaxPeople(),
                            group.getTags(),
                            currentCount,
                            courseId,
                            isOwner
                    );
                })
                .collect(Collectors.toList());
    }

    // 10. 그룹 탈퇴
    @Transactional
    public void leaveGroup(String email, Long groupId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));

        if (group.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("방장은 그룹을 나갈 수 없습니다. '그룹 삭제'를 이용해주세요.");
        }

        UserGroup userGroup = userGroupRepository.findByUserAndRunningGroup(user, group)
                .orElseThrow(() -> new IllegalArgumentException("이 그룹에 가입되어 있지 않습니다."));

        userGroupRepository.delete(userGroup);
    }
}