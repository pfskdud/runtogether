package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.*;
import runtogether.server.dto.GroupDto;
import runtogether.server.repository.*;

import java.time.LocalDate;
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

    // 1. 그룹 생성 (날짜 + 코스 완벽 처리)
    @Transactional
    public Long createGroup(String email, GroupDto.CreateRequest request) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 코스 결정 로직
        Course selectedCourse = null;

        // Case A: 직접 검색한 새로운 코스인 경우
        if (request.getCourseId() == null || request.getCourseId() == -1) {
            Double dist = 0.0;
            try {
                if (request.getDistance() != null && !request.getDistance().isEmpty()) {
                    // 숫자와 마침표(.)만 남기고 제거 (예: "1.2km" -> "1.2")
                    String distStr = request.getDistance().replaceAll("[^0-9.]", "");
                    dist = Double.parseDouble(distStr);
                }
            } catch (Exception e) { dist = 0.0; }

            Integer time = 0;
            if (request.getExpectedTime() != null) {
                try {
                    time = Integer.parseInt(request.getExpectedTime().replaceAll("[^0-9]", ""));
                } catch (Exception e) { time = 0; }
            }

            // [수정 포인트] 새 코스 생성
            selectedCourse = new Course(
                    request.getCourseTitle() != null ? request.getCourseTitle() : "검색된 코스",
                    dist,
                    time,
                    request.getPathData(),
                    null,
                    LocalDate.parse(request.getStartDate()),
                    LocalDate.parse(request.getEndDate()),
                    null ,
                    false
            );
            courseRepository.save(selectedCourse);
        }
        // Case B: 기존 코스 선택
        else {
            selectedCourse = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("코스를 찾을 수 없습니다."));
        }

        // 2. 그룹 생성 및 저장
        RunningGroup group = new RunningGroup(
                request.getGroupName(),
                request.getDescription(),
                request.isSecret(),
                request.isSearchable(),
                request.getMaxPeople(),
                request.getTags(),
                owner,
                LocalDate.parse(request.getStartDate()),
                LocalDate.parse(request.getEndDate())
        );

        group.setCourse(selectedCourse);
        RunningGroup savedGroup = groupRepository.save(group);

        // ★ [핵심 수정] 직접 만든 코스라면, 코스에도 이 그룹 정보와 날짜를 업데이트 해줍니다.
        // 이 과정이 있어야 distance 데이터가 유실되지 않고 DB에 정확히 연결됩니다.
        if (request.getCourseId() == null || request.getCourseId() == -1) {
            selectedCourse.updateGroupAndSchedule(savedGroup, savedGroup.getStartDate(), savedGroup.getEndDate());
            courseRepository.save(selectedCourse);
        }

        // 3. 방장 자동 가입
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
                group,
                false
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

    @Transactional
    public Long joinGroupByAccessCode(String email, String accessCode) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
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

    // 4. 필터링된 그룹 목록 조회
    @Transactional(readOnly = true)
    public List<GroupDto.Response> getFilteredGroups(String keyword, String status, String type) {
        List<RunningGroup> groups;
        if (keyword != null && !keyword.trim().isEmpty()) {
            groups = groupRepository.findAllByGroupNameContainingOrTagsContaining(keyword, keyword);
        } else {
            groups = groupRepository.findAll();
        }

        return groups.stream()
                .map(group -> {
                    int currentCount = userGroupRepository.countByGroupId(group.getId());
                    Long courseId = (group.getCourse() != null) ? group.getCourse().getId() : null;

                    return new GroupDto.Response(
                            group.getId(),
                            group.getGroupName(),
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
                        if (dto.getMaxPeople() != null && dto.getCurrentPeople() >= dto.getMaxPeople()) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroupDto.DetailResponse getGroupDetail(String email, Long groupId) {
        RunningGroup group = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        boolean isOwner = group.getOwner().getId().equals(user.getId());
        long dDay = (group.getStartDate() != null) ? ChronoUnit.DAYS.between(LocalDate.now(), group.getStartDate()) : 0;
        Long courseId = (group.getCourse() != null) ? group.getCourse().getId() : null;

        return new GroupDto.DetailResponse(
                group.getId(), group.getGroupName(), group.getDescription(),
                group.isSecret(), group.getAccessCode(), isOwner, courseId,
                group.getStartDate(), group.getEndDate(), dDay
        );
    }

    @Transactional
    public void updateGroup(String email, Long groupId, GroupDto.UpdateRequest request) {
        RunningGroup group = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        if (!group.getOwner().getId().equals(user.getId())) throw new IllegalArgumentException("방장만 수정할 수 있습니다.");
        group.updateInfo(request.getGroupName(), request.getDescription());
    }

    @Transactional
    public void deleteGroup(String email, Long groupId) {
        RunningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        if (!group.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("방장만 삭제할 수 있습니다.");
        }

        List<RunRecord> groupRecords = runRecordRepository.findAllByRunningGroup(group);
        runRecordRepository.deleteAll(groupRecords);

        userGroupRepository.deleteByRunningGroup(group);

        Course course = group.getCourse();
        if (course != null) {
            if (course.getRunningGroup() != null && course.getRunningGroup().getId().equals(groupId)) {
                courseRepository.delete(course);
            } else {
                course.disconnectGroup();
                courseRepository.save(course);
            }
        }

        groupRepository.delete(group);
    }

    @Transactional(readOnly = true)
    public GroupDto.MainResponse getGroupMain(String email, Long groupId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        RunningGroup group = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("그룹 없음"));

        Course mainCourse = group.getCourse();
        String courseName = (mainCourse != null) ? mainCourse.getTitle() : "등록된 코스 없음";
        Double goalDistance = (mainCourse != null) ? mainCourse.getDistance() : 0.0;
        Long courseId = (mainCourse != null) ? mainCourse.getId() : null;

        String datePeriod = group.getStartDate() + " ~ " + group.getEndDate();
        long days = ChronoUnit.DAYS.between(LocalDate.now(), group.getStartDate());
        String dDayString = (days > 0) ? "D-" + days : (days == 0 ? "D-Day" : "D+" + Math.abs(days));

        return new GroupDto.MainResponse(
                group.getGroupName(), courseName, datePeriod, dDayString,
                user.getNickname(), 0.0, goalDistance, "dummy_url", courseId
        );
    }

    @Transactional(readOnly = true)
    public List<GroupDto.Response> getMyGroups(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        return userGroupRepository.findAllByUser(user).stream()
                .map(ug -> {
                    RunningGroup g = ug.getRunningGroup();
                    return new GroupDto.Response(
                            g.getId(), g.getGroupName(), g.getDescription(), g.isSecret(),
                            g.getOwner().getNickname(), g.getMaxPeople(), g.getTags(),
                            userGroupRepository.countByGroupId(g.getId()),
                            (g.getCourse() != null ? g.getCourse().getId() : null),
                            g.getOwner().getId().equals(user.getId())
                    );
                }).collect(Collectors.toList());
    }

    @Transactional
    public void leaveGroup(String email, Long groupId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        RunningGroup group = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("그룹 없음"));
        if (group.getOwner().getId().equals(user.getId())) throw new IllegalArgumentException("방장은 나갈 수 없습니다.");

        UserGroup ug = userGroupRepository.findByUserAndRunningGroup(user, group)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 그룹"));
        userGroupRepository.delete(ug);
    }
}