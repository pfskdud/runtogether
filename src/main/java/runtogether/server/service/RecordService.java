package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.*; // 여기에 RunRecord, User, Course 등이 다 들어있음
import runtogether.server.dto.RecordDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordService {

    // 이름이 RunRecordRepository로 바뀌었을 수 있습니다. 확인해 보세요!
    // 만약 리팩터링이 리포지토리 이름까지 안 바꿨다면 RecordRepository 그대로 두셔도 됩니다.
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    // 1. 기록 저장 기능
    @Transactional
    public Long createRecord(String email, RecordDto.Request requestDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        Course course = courseRepository.findById(requestDto.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("코스 없음"));

        // ★ 여기가 수정되었습니다 (Record -> RunRecord)
        RunRecord record = new RunRecord(user, course, requestDto.getRunTime());

        return recordRepository.save(record).getId();
    }

    // 2. 내 기록 조회 기능
    @Transactional(readOnly = true)
    public List<RecordDto.Response> getMyRecords(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));

        return recordRepository.findAllByUser(user).stream()
                .map(record -> new RecordDto.Response(
                        record.getId(),
                        record.getCourse().getTitle(),
                        record.getRunTime(),
                        record.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }
}