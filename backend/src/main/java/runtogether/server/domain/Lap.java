package runtogether.server.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "laps")
public class Lap {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lapId; // PK

    // 어떤 기록에 속하는지 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private RunRecord runRecord;

    private int lapKm;      // 1km 지점
    private double lapPace; // 6.41 (double 타입)
    private int lapTime;    // 401초 (int 타입)

    // 생성자 (RecordService에서 이 순서대로 호출함)
    public Lap(RunRecord runRecord, int lapKm, double lapPace, int lapTime) {
        this.runRecord = runRecord;
        this.lapKm = lapKm;
        this.lapPace = lapPace;
        this.lapTime = lapTime;
    }
}