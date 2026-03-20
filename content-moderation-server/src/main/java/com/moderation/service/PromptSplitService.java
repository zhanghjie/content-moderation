package com.moderation.service;

import com.moderation.model.req.PromptSplitReq;
import com.moderation.model.res.PromptSplitRes;

public interface PromptSplitService {
    PromptSplitRes split(PromptSplitReq req);
}
