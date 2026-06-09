package com.kuky.backend.scheduling.meeting;

import com.kuky.backend.config.SchedulingProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeetingProviderConfig {

    @Bean
    public MeetingProvider meetingProvider(SchedulingProperties props) {
        String clientId = props.getZoom().getClientId();
        if (clientId == null || clientId.isBlank()) {
            return new StubMeetingProvider();
        }
        return new ZoomMeetingProvider(props);
    }
}
