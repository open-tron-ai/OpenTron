//! MemoryBackend trait for all storage backends.

use OpenTron_core::{OpenTronError, RetrievalResult};
use serde_json::Value;

pub trait MemoryBackend: Send + Sync {
    fn backend_id(&self) -> &str;
    fn store(
        &self,
        content: &str,
        source: &str,
        metadata: Option<&Value>,
    ) -> Result<String, OpenTronError>;
    fn retrieve(
        &self,
        query: &str,
        top_k: usize,
    ) -> Result<Vec<RetrievalResult>, OpenTronError>;
    fn delete(&self, doc_id: &str) -> Result<bool, OpenTronError>;
    fn clear(&self) -> Result<(), OpenTronError>;
    fn count(&self) -> Result<usize, OpenTronError>;
}

