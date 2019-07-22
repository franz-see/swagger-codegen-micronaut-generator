package ph.net.see.swaggercodegenmicronautgenerator.lambda;

import com.github.jknack.handlebars.Lambda;
import com.github.jknack.handlebars.Template;
import io.swagger.codegen.v3.generators.handlebars.lambda.LowercaseLambda;

import java.io.IOException;

public class CapitaliseLambda extends LowercaseLambda implements Lambda {

    @Override
    public Object apply(Object o, Template template) throws IOException {
        String text = (String) super.apply(o, template);
        if (text.length() == 1) {
            text = String.valueOf(Character.toUpperCase(text.charAt(0)));
        } else if (text.length() > 1) {
            text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
        }
        return text;
    }
}
