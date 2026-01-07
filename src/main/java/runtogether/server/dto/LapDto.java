package runtogether.server.dto;

import lombok.Data;
import runtogether.server.domain.Lap;

@Data
public class LapDto {
    private int km;
    private double pace;
    private int time; // 초 단위

    public LapDto(Lap lap) {
        this.km = lap.getLapKm();
        this.pace = lap.getLapPace();
        this.time = lap.getLapTime();
    }
}