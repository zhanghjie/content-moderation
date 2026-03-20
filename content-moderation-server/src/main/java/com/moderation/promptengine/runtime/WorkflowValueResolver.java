package com.moderation.promptengine.runtime;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WorkflowValueResolver {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*}}");

    public Object resolveTemplateValue(String template, WorkflowRuntimeContext runtimeContext) {
        if (template == null) return null;
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        if (matcher.matches()) {
            return resolveExpression(matcher.group(1), runtimeContext);
        }
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            result.append(template, cursor, matcher.start());
            Object value = resolveExpression(matcher.group(1), runtimeContext);
            result.append(value == null ? "" : value);
            cursor = matcher.end();
        }
        result.append(template.substring(cursor));
        return result.toString();
    }

    public Object resolveExpression(String expression, WorkflowRuntimeContext runtimeContext) {
        if (expression == null) return null;
        String expr = expression.trim();
        if (expr.startsWith("context.")) return pickByPath(runtimeContext.getContext(), expr.substring("context.".length()));
        if (expr.startsWith("external.")) return pickByPath(runtimeContext.getExternal(), expr.substring("external.".length()));
        if (expr.endsWith(".output")) {
            String nodeId = expr.substring(0, expr.length() - ".output".length());
            Map<String, Object> node = runtimeContext.getNodes().get(nodeId);
            return node == null ? null : node.get("output");
        }
        if (runtimeContext.getOutputs().containsKey(expr)) return runtimeContext.getOutputs().get(expr);
        if (runtimeContext.getContext().containsKey(expr)) return runtimeContext.getContext().get(expr);
        return evaluateSimpleExpression(expr, runtimeContext);
    }

    public boolean resolveBoolean(String expression, WorkflowRuntimeContext runtimeContext) {
        Object value = resolveExpression(expression, runtimeContext);
        if (value instanceof Boolean b) return b;
        if (value instanceof Number number) return number.doubleValue() > 0;
        return value != null && !"false".equalsIgnoreCase(String.valueOf(value).trim());
    }

    private Object evaluateSimpleExpression(String expr, WorkflowRuntimeContext runtimeContext) {
        String plain = expr.replace(" ", "");
        if (plain.startsWith("len(") && plain.endsWith(")")) {
            Object value = resolveExpression(plain.substring(4, plain.length() - 1), runtimeContext);
            return value == null ? 0 : String.valueOf(value).length();
        }
        String[] operators = new String[]{">=", "<=", "==", "!=", ">", "<"};
        for (String operator : operators) {
            int index = plain.indexOf(operator);
            if (index < 0) continue;
            Object left = evaluateSimpleExpression(plain.substring(0, index), runtimeContext);
            Object right = evaluateSimpleExpression(plain.substring(index + operator.length()), runtimeContext);
            return compare(left, right, operator);
        }
        if (plain.endsWith(".length")) {
            Object value = resolveExpression(plain.substring(0, plain.length() - ".length".length()), runtimeContext);
            return value == null ? 0 : String.valueOf(value).length();
        }
        if (plain.matches("-?\\d+")) return Long.parseLong(plain);
        if (plain.matches("-?\\d+\\.\\d+")) return Double.parseDouble(plain);
        if ((plain.startsWith("'") && plain.endsWith("'")) || (plain.startsWith("\"") && plain.endsWith("\""))) {
            return plain.substring(1, plain.length() - 1);
        }
        return null;
    }

    private boolean compare(Object left, Object right, String operator) {
        if (left instanceof Number ln && right instanceof Number rn) {
            double l = ln.doubleValue();
            double r = rn.doubleValue();
            return switch (operator) {
                case ">" -> l > r;
                case "<" -> l < r;
                case ">=" -> l >= r;
                case "<=" -> l <= r;
                case "==" -> l == r;
                case "!=" -> l != r;
                default -> false;
            };
        }
        String l = left == null ? "" : String.valueOf(left);
        String r = right == null ? "" : String.valueOf(right);
        return switch (operator) {
            case "==" -> l.equals(r);
            case "!=" -> !l.equals(r);
            case ">" -> l.compareTo(r) > 0;
            case "<" -> l.compareTo(r) < 0;
            case ">=" -> l.compareTo(r) >= 0;
            case "<=" -> l.compareTo(r) <= 0;
            default -> false;
        };
    }

    private Object pickByPath(Map<String, Object> source, String path) {
        if (source == null || path == null || path.isBlank()) return null;
        String[] parts = path.split("\\.");
        Object current = source;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(part);
        }
        return current;
    }
}
