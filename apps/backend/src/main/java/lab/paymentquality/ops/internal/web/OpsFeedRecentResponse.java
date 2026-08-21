package lab.paymentquality.ops.internal.web;

import lab.paymentquality.ops.OpsFeedFrame;

import java.util.List;

public record OpsFeedRecentResponse(List<OpsFeedFrame> events) {
}
