import com.hit.spring.SpringStarterConfig;
import com.hit.spring.core.extension.streaming.DataStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest(classes = SpringStarterConfig.class)
public class DataStreamTest {

    private static final List<String> DATA = List.of("A", "B", "C", "D", "F", "G", "H", "J", "K");

    @Test
    public void testFlowable() {
        List<String> actualData = new ArrayList<>();
        DataStream<List<String>> streaming = streaming();

        streaming.subscribe(
                batch -> {
                    for (String data : batch) {
                        log.info("[testFlowable] Batch: {}", data);
                        actualData.add(data);
                    }
                },
                error -> {
                    log.error("[testFlowable] Error while processing data: {}", error.getMessage(), error);
                },
                () -> {
                    log.info("[testFlowable] Success");
                }
        );
        log.info("[testFlowable] ActualData: {}", actualData);
        Assertions.assertEquals(DATA, actualData);
    }

    public DataStream<List<String>> streaming() {
        return DataStream.create(emitter -> {
            for (String data : DATA) {
                try {
                    List<String> nextData = new ArrayList<>();
                    nextData.add(data);
                    emitter.onNext(nextData);
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
            emitter.onComplete();
        });
    }
}
