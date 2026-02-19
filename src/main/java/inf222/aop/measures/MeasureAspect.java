package inf222.aop.measures;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

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


    @Pointcut("get(* *_*) && !within(MeasureAspect)")
    public void measureFieldGet() {}


    @Around("measureFieldGet()")
    public Object convertToMeter(ProceedingJoinPoint jp) throws Throwable {
        String fN = jp.getSignature().getName();
        Matcher match = pattern.matcher(fN);
        Object raw = jp.proceed();

        if (!match.matches() || !(raw instanceof Number)) {
            return raw;
        }

        String unit = match.group(1);
        Double factor = toMeter.get(unit);
        if (factor == null) {
            return raw;
        }

        double value = ((Number) raw).doubleValue();
        return value * factor;
    }


    @Pointcut("set(* *_*) && !within(MeasureAspect) && !cflow(execution(*.new(..)))")
    public void measureFieldSetOne() {}


    @Pointcut("set(* *_*) && !within(MeasureAspect)")
    public void MeasureFieldSet() {}


    @Around("measureFieldSetOne()")
    public void convertBackFromMeter(ProceedingJoinPoint jp) throws Throwable {
        Object arg = jp.getArgs()[0];
        if (!(arg instanceof Number)) {
            jp.proceed();
            return;
        }
        double valueInMeters = ((Number) arg).doubleValue();

        String fN = jp.getSignature().getName();
        Matcher match = pattern.matcher(fN);

        if (match.matches()) {
            String unit = match.group(1);
            Double factor = toMeter.get(unit);
            if (factor != null) {
                jp.proceed(new Object[]{ valueInMeters / factor });
                return;
            }
        }
        jp.proceed();
    }


    @Before("MeasureFieldSet()")
    public void preventNegative(JoinPoint jp) {
        Object arg = jp.getArgs()[0];
        if (arg instanceof Number number && number.doubleValue() < 0) {
            throw new Error("Illegal modification");
        }
    }




}