package inf222.aop.measures;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect   // modify method calls or field access without modifying the original code
public class MeasureAspect {
    private final String regex;
    private final Pattern pattern;

    private final Map<String, Double> toMeter = new HashMap<String, Double>(Map.of(
            "m", 1d,
            "ft", 0.3048d,
            "in", 0.0254d,
            "cm", 0.01d,
            "yd", 0.9144d));

    public MeasureAspect() {
        String elems = String.join("|", toMeter.keySet());
        regex = String.format(".*_(%s)$", elems);
        pattern = Pattern.compile(regex);
    }

    // Intercepts any get access to a double field in Measures
    @Around("get(double inf222.aop.measures.Measures.*)")
    public double convertToMeters(ProceedingJoinPoint pjp) throws Throwable {
        String fieldName = pjp.getSignature().getName();
        Matcher matcher = pattern.matcher(fieldName);

        if (matcher.matches()) {    // Checks if the field name ends with a  unit.
            String unit = matcher.group(1);
            Double factor = toMeter.get(unit);

            double originalValue = (Double) pjp.proceed();  //Call the original getter

            return originalValue * factor;
        }

        return (Double) pjp.proceed();   //Return value
    }

    // Intercepts any set access to a double field in Measures
    @Around("set(double inf222.aop.measures.Measures.*) && !withincode(inf222.aop.measures.Measures.new(..))")
    public void convertFromMeters(ProceedingJoinPoint pjp) throws Throwable {
        String fieldName = pjp.getSignature().getName();
        Matcher matcher = pattern.matcher(fieldName);

        if (matcher.matches()) {   // Checks if the field name ends with a  unit.
            String unit = matcher.group(1);
            Double factor = toMeter.get(unit);

            double valueInMeters = (Double) pjp.getArgs()[0];

            if (valueInMeters < 0) {        // If value is negative
                throw new Error("Illegal modification");
            }

            double valueInOriginalUnit = valueInMeters / factor;   // from meters to the original unit before storing

            pjp.proceed(new Object[]{valueInOriginalUnit});   // Calls the original setter with the converted value.
        } else {
            pjp.proceed();
        }
    }


}