package org.opentron.backend.approvals;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ApprovalsService {

    private final List<String> pending = Collections.synchronizedList(new ArrayList<>());

    public List<String> listPending() {
        return new ArrayList<>(pending);
    }

    public void addPending(String id) {
        pending.add(id);
    }

    public boolean approve(String id) {
        return pending.remove(id);
    }
}
