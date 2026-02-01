package runtogether.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import runtogether.server.domain.Lap;
import runtogether.server.domain.RunRecord;
import runtogether.server.repository.RunRecordRepository;
import runtogether.server.dto.RankingDto;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RunRecordRepository recordRepository;

    // 랭킹 조회 메인 메서드 (★ courseId -> groupId 로 변경됨)
    @Transactional(readOnly = true)
    public List<RankingDto> getRanking(String email, Long groupId, String type, Integer km) {

        List<RankingDto> result = new ArrayList<>();

        if ("TOTAL".equals(type)) {
            // 1. 시간순 (전체 기록) 랭킹 (★ groupId 기준 조회)
            List<RunRecord> records = recordRepository.findRankingByTotalTime(groupId);

            for (int i = 0; i < records.size(); i++) {
                RunRecord r = records.get(i);
                boolean isMe = r.getUser().getEmail().equals(email);

                result.add(new RankingDto(
                        i + 1, // 등수 (0부터 시작하니까 +1)
                        r.getUser().getNickname(),
                        r.getUser().getProfileImageUrl(),
                        r.getRunTime(), // "56:42"
                        isMe
                ));
            }

        } else if ("SECTION".equals(type)) {
            // 2. 구간순 (1km, 2km...) 랭킹
            if (km == null) km = 1; // km 안 보내면 기본 1km로 설정

            // (★ groupId 기준 조회)
            List<Lap> laps = recordRepository.findRankingBySection(groupId, km);

            for (int i = 0; i < laps.size(); i++) {
                Lap l = laps.get(i);
                boolean isMe = l.getRunRecord().getUser().getEmail().equals(email);

                // 초 단위(int)를 "분'초"" 포맷으로 변환 (예: 401초 -> 6'41")
                String formattedTime = convertSecToTime(l.getLapTime());

                result.add(new RankingDto(
                        i + 1,
                        l.getRunRecord().getUser().getNickname(),
                        l.getRunRecord().getUser().getProfileImageUrl(),
                        formattedTime,
                        isMe
                ));
            }
        }

        return result;
    }

    // [보조] 초 -> "6'41"" 변환 메서드
    private String convertSecToTime(int totalSeconds) {
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format("%d'%02d\"", min, sec);
    }
}