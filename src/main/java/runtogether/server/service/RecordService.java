package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.*; // RunRecord, User, Course 등
import runtogether.server.dto.RecordDto;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordService {

    // ★ 리포지토리 이름이 RecordRepository인지 RunRecordRepository인지 확인해서 맞추세요!
    private final RunRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    // 1. 기록 저장 (수정됨: 8가지 정보 모두 저장)
    @Transactional
    public Long createRecord(String email, RecordDto.Request requestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Course course = courseRepository.findById(requestDto.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("코스 없음"));

        // ★ [핵심] RunRecord 생성자에 8개 데이터를 다 넣어줍니다.
        RunRecord record = new RunRecord(
                user,
                course,
                requestDto.getRunTime(),     // 시간
                requestDto.getDistance(),    // 거리
                requestDto.getAveragePace(), // 페이스
                requestDto.getHeartRate(),   // 심박수
                requestDto.getCalories(),    // 칼로리
                requestDto.getSectionJson()  // 구간/그래프 데이터
        );

        return recordRepository.save(record).getId();
    }

    // 2. 내 기록 목록 조회
    @Transactional(readOnly = true)
    public List<RecordDto.Response> getMyRecords(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        return recordRepository.findAllByUser(user).stream()
                .map(record -> new RecordDto.Response(
                        record.getId(),
                        record.getCourse().getTitle(),
                        record.getRunTime(),
                        record.getCreatedAt().toLocalDate().toString()
                ))
                .collect(Collectors.toList());
    }

    // 3. 기록 상세 조회 (랭킹 계산 포함 - 아까 만든 로직)
    @Transactional(readOnly = true)
    public RecordDto.DetailResponse getRecordDetail(String email, Long recordId) {
        RunRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("기록이 존재하지 않습니다."));

        // 본인 확인
        if (!record.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인의 기록만 확인할 수 있습니다.");
        }

        Course course = record.getCourse();
        RunningGroup group = course.getRunningGroup();

        // 랭킹 계산
        int totalRunners = recordRepository.countByCourse(course);
        int myRank = recordRepository.countByCourseAndRunTimeLessThan(course, record.getRunTime()) + 1;

        // 날짜 포맷팅
        String date = record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        String startTime = record.getCreatedAt().format(DateTimeFormatter.ofPattern("h:mm a"));

        return new RecordDto.DetailResponse(
                group != null ? group.getName() : "개별 코스",
                record.getRunTime(),
                date,
                startTime,
                record.getDistance(),
                record.getAveragePace(),
                record.getHeartRate(),
                record.getCalories(),
                record.getSectionJson(),
                myRank,
                totalRunners,
                "완주 성공 배지"
        );
    }
}