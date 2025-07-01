import com.hit.spring.SpringStarterConfig;
import com.hit.spring.template.StringTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.context.Context;

@Slf4j
@SpringBootTest(classes = SpringStarterConfig.class)
public class TemplateTest {

    @Autowired
    private StringTemplateService stringTemplateService;

    @Test
    public void test() {

        String htmlTemplate = """
                <ul type="square">
                    <li>
                        Tổng giá trị danh mục sao chép của Bạn là <span th:text="${aum}"></span> VND -\s
                        <span th:if="${#strings.startsWith(amount, '-')}">
                            giảm <span style="color: red;" th:text="${amount}"></span>
                        </span>
                        <span th:unless="${#strings.startsWith(amount, '-')}">
                            tăng <span style="color: green;" th:text="'+' + ${amount}"></span>
                        </span>
                        so với tuần trước, tương đương tỷ suất lợi nhuận
                    </li>
                </ul>
            """;

        Context context = new Context();
        context.setVariable("aum", "1,000,000");
        context.setVariable("amount", "-41,580,321");

        String s = stringTemplateService.processFromString(htmlTemplate, context);
        log.info(s);

    }


}