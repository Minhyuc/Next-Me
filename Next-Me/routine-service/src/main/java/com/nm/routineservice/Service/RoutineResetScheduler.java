package com.nm.routineservice.Service;

import com.nm.routineservice.Repository.RoutineSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
/** 1분마다 사용자 설정을 확인하여 필요한 사용자만 새 루틴 날짜로 넘긴다. */
public class RoutineResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoutineResetScheduler.class);

    private final RoutineSettingRepository settingRepository;
    private final RoutineService routineService;

    public RoutineResetScheduler(
            RoutineSettingRepository settingRepository,
            RoutineService routineService
    ) {
        this.settingRepository = settingRepository;
        this.routineService = routineService;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    // 실제 초기화 시각 판단은 RoutineService가 시간대와 resetTime을 이용해 수행한다.
    public void resetDueRoutines() {
        settingRepository.findAll().forEach(setting -> {
            try {
                routineService.rollOverUser(setting.getUserId());
            } catch (IllegalStateException exception) {
                // 아직 최초 루틴을 만들지 않은 사용자는 초기화할 항목이 없다.
                log.debug("Routine rollover skipped for user {}: {}",
                        setting.getUserId(), exception.getMessage());
            } catch (Exception exception) {
                log.error("Routine rollover failed for user {}", setting.getUserId(), exception);
            }
        });
    }
}
