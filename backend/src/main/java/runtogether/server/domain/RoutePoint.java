package runtogether.server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class RoutePoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기존 RunRecord와 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_record_id")
    private RunRecord runRecord;

    private double latitude;  // 위도
    private double longitude; // 경도
    private int elapsedSeconds; // 0초, 1초, 2초... (리플레이의 핵심)

    // 생성자나 빌더 패턴 추가
    public RoutePoint(RunRecord runRecord, double latitude, double longitude, int elapsedSeconds) {
        this.runRecord = runRecord;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elapsedSeconds = elapsedSeconds;
    }
}