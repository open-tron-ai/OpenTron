# OpenTron Implementation TODOs from Code Review

## STATUS: PARTIALLY IMPLEMENTED
- ✅ Preserve chat history in Tron coordinator mode
- ✅ Fix coordinator progress and final response handling  
- ✅ Improve coordinator backend handling safely
- ✅ Add missing backend route stubs (analytics, agents/events, connectors, managed-agents)
- ⏳ **REMAINING: Core refactoring work** (see below)

---

## Frontend Issues (Priority: HIGH)

### 1. Refactor api.ts - Shared Settings & Header Handling
- [ ] Create `loadSettings()` helper to avoid duplicate localStorage parsing
- [ ] Normalize headers using `new Headers(init.headers)`
- [ ] Fix `authHeaders()` to not mutate input object
- [ ] Remove duplication in `getSettingsApiUrl()` and `getApiKey()`
- **Files**: `frontend/src/lib/api.ts`
- **Impact**: Reduces brittleness, improves maintainability

### 2. Extract App.tsx Side Effects into Custom Hooks
- [ ] Create `useTheme` hook
- [ ] Create `useModelFetch` hook
- [ ] Create `useServerInfo` hook
- [ ] Create `useSavingsPolling` hook
- [ ] Create `useOverlayImport` hook
- [ ] Create `useKeyboardShortcuts` hook
- [ ] Move opt-in modal logic to separate hook
- **Files**: `frontend/src/App.tsx`
- **Impact**: Reduces complexity, improves testability

### 3. Refactor InputArea.tsx into Smaller Components/Hooks
- [ ] Extract `useChatStream` hook
- [ ] Extract `useTextAreaAutoResize` hook
- [ ] Extract `useSpeechInput` hook
- [ ] Extract `useCorpusSync` hook
- [ ] Split into separate sub-components if needed
- **Files**: `frontend/src/components/Chat/InputArea.tsx`
- **Impact**: Component becomes more maintainable and testable

### 4. Add Cancellation Support & Async Cleanup
- [ ] Use `AbortController` for all fetch calls in effects
- [ ] Guard state updates against unmount race conditions
- [ ] Move polling into reusable hooks with proper cleanup
- [ ] Check `App.tsx` and `SetupScreen.tsx` for missing guards
- **Files**: 
  - `frontend/src/App.tsx`
  - `frontend/src/components/Setup/SetupScreen.tsx`
  - `frontend/src/lib/sse.ts`
- **Impact**: Prevents memory leaks and race conditions

### 5. Clean Up sse.ts - Logging & Parser Robustness
- [ ] Remove or gate console.log() statements behind debug flag
- [ ] Consider using `TextDecoderStream` instead of custom SSE parser
- [ ] Improve chunk boundary handling
- [ ] Standardize error messages and throw typed errors
- [ ] Fix `chatCompletionSimple()` production logging
- **Files**: `frontend/src/lib/sse.ts`
- **Impact**: Cleaner logs, more robust streaming

### 6. Split Zustand Store into Slices
- [ ] Create `conversationStore` slice
- [ ] Create `uiStore` slice
- [ ] Create `serverStore` slice
- [ ] Create `agentStore` slice
- [ ] Update all store references throughout app
- **Files**: 
  - `frontend/src/lib/store.ts` (refactor)
  - All files importing store
- **Impact**: Reduces accidental re-renders, improves maintainability

### 7. Standardize Storage Key Naming
- [ ] Audit all localStorage keys across codebase
- [ ] Change `oj-setup-completed` to `opentron-setup-completed` pattern
- [ ] Use consistent `opentron-*` prefix everywhere
- **Files**: Multiple (search for localStorage)
- **Impact**: Easier debugging, consistent conventions

### 8. Fix Dependency Mismatch
- [ ] Verify `react-router` vs `react-router-dom` issue
- [ ] Switch to `react-router-dom` if needed in `main.tsx`
- [ ] Update `package.json` if necessary
- **Files**: 
  - `frontend/package.json`
  - `frontend/src/main.tsx`
- **Impact**: Correct dependency tree

### 9. Improve Error Handling in Frontend
- [ ] Add logging in `api.ts` Tauri fallback code
- [ ] Add logging in `sse.ts` malformed chunk handler
- [ ] Add logging in `InputArea.tsx` audio/stream failure handlers
- [ ] Preserve meaningful error details when rethrowing
- **Files**: 
  - `frontend/src/lib/api.ts`
  - `frontend/src/lib/sse.ts`
  - `frontend/src/components/Chat/InputArea.tsx`
- **Impact**: Better debugging in production

---

## Backend Issues (Priority: HIGH)

### 10. Replace System.out/err with Proper Logging
- [ ] Add SLF4J dependency if not present
- [ ] Create logger in every controller and service
- [ ] Replace all `System.out.println()` with logger calls
- [ ] Replace all `System.err.println()` with logger.error() or logger.warn()
- **Files**: 
  - `OpentronBackendApplication.java`
  - `ChatController.java`
  - `AgentsController.java`
  - `MultiAgentCoordinator.java`
  - `ReactiveChatWebSocketHandler.java`
  - `StorageService.java`
  - And many others (~27 controller files)
- **Impact**: Production-ready logging, easier debugging

### 11. Implement @RestControllerAdvice for Error Handling
- [ ] Create `GlobalExceptionHandler` class with `@RestControllerAdvice`
- [ ] Handle common exceptions (validation, not found, server errors)
- [ ] Return proper error response DTOs instead of raw JSON strings
- [ ] Ensure consistent HTTP status codes and response format
- **Files**: 
  - Create: `org/opentron/backend/exception/GlobalExceptionHandler.java`
  - Create: `org/opentron/backend/dto/ErrorResponse.java`
- **Impact**: Consistent error handling across all controllers

### 12. Replace Raw Map with DTO Classes
- [ ] Create request DTOs for each controller
  - `ChatRequest.java` (for chat endpoint)
  - `CoordinateAgentRequest.java` (for coordinator)
  - `SendAgentTaskRequest.java` (for agent tasks)
  - And others as needed
- [ ] Add `@Validated` and `@JsonProperty` annotations
- [ ] Add proper validation constraints
- [ ] Update all controllers to use DTOs
- **Files**: 
  - Create: `org/opentron/backend/dto/` package
  - Update: All controller files
- **Impact**: Type-safe request handling, better validation

### 13. Refactor ChatController - Reduce Responsibility
- [ ] Extract `StreamingResponseService` for SSE construction
- [ ] Extract `EngineRoutingService` for model/engine selection
- [ ] Move telemetry recording to a separate service layer
- [ ] Consolidate duplicate logic in `handleCloudModelChat`, `handleHuggingFaceChat`, `handleOllamaCliChat`
- [ ] Inject telemetry instead of servlet context lookup
- [ ] Remove header forwarding complexity or move to utility
- **Files**: 
  - `ChatController.java` (refactor)
  - Create: `org/opentron/backend/services/StreamingResponseService.java`
  - Update: `EngineRouting.java`
- **Impact**: Easier to test, maintain, and extend

### 14. Fix Reactive/Imperative Code Mixing
- [ ] Remove `.block()` calls from `EngineRouting.detectEngineType()`
- [ ] Move blocking operations to separate non-reactive method
- [ ] Refactor `ReactiveChatWebSocketHandler` to use proper Reactor operators
- [ ] Fix `AgentsController.coordinateAgents()` to use proper Flux operators instead of manual threading
- **Files**: 
  - `EngineRouting.java`
  - `ReactiveChatWebSocketHandler.java`
  - `AgentsController.java`
- **Impact**: Better performance, proper reactive patterns

### 15. Fix Thread Management - Use Spring Executors
- [ ] Create `TaskExecutor` bean in Spring config
- [ ] Replace `new Thread(...)` with `@Async` or executor service
- [ ] Fix `MultiAgentCoordinator.startMessageProcessor()` to use Spring scheduler
- [ ] Fix cleanup threads in controllers to use Spring lifecycle hooks
- **Files**: 
  - Create or update: Spring configuration class
  - `MultiAgentCoordinator.java`
  - `AgentsController.java`
  - `ModelPreloader.java`
  - `ManagedAgentsController.java`
- **Impact**: Proper resource management, easier lifecycle handling

### 16. Improve SSE and Streaming Support
- [ ] Stop fabricating SSE chunks from space splitting
- [ ] Preserve native streaming from underlying engines
- [ ] Improve `ReactiveChatWebSocketHandler` SSE parsing
- [ ] Use `TextDecoderStream` or similar for robust chunk handling
- [ ] Test partial packet handling edge cases
- **Files**: 
  - `ChatController.java`
  - `ReactiveChatWebSocketHandler.java`
  - `EngineRouting.java`
- **Impact**: Better streaming reliability

### 17. Fix Engine Routing Detection
- [ ] Remove `.block()` or isolate behind startup probe
- [ ] Make detection deterministic (avoid race conditions)
- [ ] Implement robust fallback logic
- [ ] Add caching for routing decisions
- [ ] Test edge cases (offline, partial availability)
- **Files**: `EngineRouting.java`
- **Impact**: More reliable model routing

### 18. Improve Data Persistence
- [ ] Tighten exception types in `StorageService` (use specific exceptions)
- [ ] Use `StandardCharsets.UTF_8` instead of `getBytes("UTF-8")`
- [ ] Implement database-level deduplication instead of in-memory hash lookup
- [ ] Add transaction management for trace saves
- [ ] Fix concurrency issues in duplicate detection
- **Files**: `StorageService.java`
- **Impact**: Better data integrity, thread safety

### 19. Fix Header Handling in API Forwarding
- [ ] Use proper `new Headers()` normalization
- [ ] Sanitize headers before forwarding
- [ ] Handle edge cases in header merging
- [ ] Remove unsafe mutation of input objects
- **Files**: 
  - `ChatController.java`
  - `EngineRouting.java`
- **Impact**: Safer header forwarding

---

## Testing & Validation (Priority: MEDIUM)

### 20. Integration Tests for Coordinator
- [ ] Test multi-agent request with history
- [ ] Verify progress events are streamed correctly
- [ ] Check final response preservation
- [ ] Test error handling in coordinator
- **Files**: Create test classes in `src/test/java`

### 21. Frontend Component Tests
- [ ] Test InputArea with Tron mode
- [ ] Test coordinator callback handling
- [ ] Test async cleanup and AbortController
- [ ] Test history passing to coordinator
- **Files**: Create test files in `frontend/src/**/*.test.ts`

### 22. E2E Tests for Full Workflow
- [ ] Test chat with regular model
- [ ] Test coordinator with multiple agents
- [ ] Test streaming and partial responses
- [ ] Test error scenarios

---

## Documentation (Priority: LOW)

### 23. Update Architecture Documentation
- [ ] Document coordinator workflow
- [ ] Document new DTO structure
- [ ] Document error handling approach
- [ ] Document logging conventions

### 24. Add Code Comments
- [ ] Document complex reactive flows
- [ ] Add JSDoc to critical functions
- [ ] Explain concurrency handling

---

## Implementation Strategy

**Phase 1 (Immediate)**: Already done
- ✅ Coordinator history and progress handling
- ✅ Missing route stubs

**Phase 2 (1-2 weeks)**: Core Quality Improvements
- Priority: Backend logging & error handling (#10, #11, #12)
- Priority: Frontend refactoring prep (#1, #4)

**Phase 3 (2-3 weeks)**: Major Refactorings
- ChatController reduction (#13)
- React patterns cleanup (#2, #3, #6)
- Threading fixes (#15)

**Phase 4 (3-4 weeks)**: Advanced Improvements
- Streaming robustness (#16, #17)
- Persistence improvements (#18)
- Testing (#20, #21, #22)

---

## Estimated Effort
- **Backend Logging**: 4-6 hours
- **Error Handling**: 3-4 hours
- **DTOs & Validation**: 4-5 hours
- **ChatController Refactor**: 6-8 hours
- **Frontend Refactoring**: 8-10 hours
- **Testing**: 6-8 hours
- **Total**: ~35-45 hours of focused work

---

## Success Criteria
1. All System.out/err replaced with proper logging
2. All controllers use error handler and return proper DTOs
3. ChatController complexity reduced by 40%+
4. Coordinator workflow end-to-end working with history
5. 80%+ test coverage on critical paths
6. No blocking calls in reactive code
7. Proper Spring lifecycle management for all async operations
