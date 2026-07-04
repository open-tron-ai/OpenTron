import { useState, useRef } from 'react';
import { Image as ImageIcon, X } from 'lucide-react';
import { toast } from 'sonner';
import { analyzeScreenshot } from '../../lib/api';

interface ImageUploadButtonProps {
  onImageAnalyze: (analysis: string, suggestions: string[]) => void;
  disabled?: boolean;
}

export function ImageUploadButton({ onImageAnalyze, disabled = false }: ImageUploadButtonProps) {
  const [loading, setLoading] = useState(false);
  const [preview, setPreview] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImageSelect = async (file: File) => {
    try {
      setLoading(true);

      // Validate file
      if (!file.type.startsWith('image/')) {
        toast.error('Please select an image file');
        return;
      }

      if (file.size > 5 * 1024 * 1024) {
        toast.error('Image must be less than 5MB');
        return;
      }

      // Convert to base64
      const reader = new FileReader();
      reader.onload = async (e) => {
        const base64 = e.target?.result as string;
        setPreview(base64);

        // Send to backend for analysis
        try {
          toast.loading('Analyzing screenshot with LLaVA...', { id: 'screenshot-analysis' });
          const result = await analyzeScreenshot(
            base64,
            'Analyze this screenshot and suggest improvements',
            ''
          );

          toast.dismiss('screenshot-analysis');

          if (result.status === 'completed') {
            const analysis = result.analysis || '';
            const suggestions = result.suggestions || [];
            onImageAnalyze(analysis, suggestions);
            toast.success('Screenshot analyzed!');
            setPreview(null);
          } else {
            toast.error(result.error || 'Analysis failed');
          }
        } catch (err: any) {
          toast.dismiss('screenshot-analysis');
          const msg = err?.message || 'Failed to analyze screenshot';
          toast.error(msg);
        }
      };
      reader.readAsDataURL(file);
    } catch (err: any) {
      toast.error(err?.message || 'Failed to process image');
    } finally {
      setLoading(false);
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleImageSelect(file);
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    const items = e.clipboardData?.items;
    if (!items) return;

    for (let i = 0; i < items.length; i++) {
      if (items[i].kind === 'file' && items[i].type.startsWith('image/')) {
        e.preventDefault();
        const file = items[i].getAsFile();
        if (file) {
          handleImageSelect(file);
        }
        break;
      }
    }
  };

  return (
    <div className="relative">
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={handleFileInputChange}
        className="hidden"
      />

      <button
        onClick={() => fileInputRef.current?.click()}
        disabled={disabled || loading}
        className="p-2 rounded-xl transition-colors shrink-0 cursor-pointer disabled:opacity-30 disabled:cursor-default"
        style={{
          background: 'var(--color-bg-tertiary)',
          color: 'var(--color-text-tertiary)',
        }}
        title="Upload image (or Ctrl+V to paste)"
        onPaste={handlePaste}
      >
        <ImageIcon size={16} />
      </button>

      {preview && (
        <div className="absolute bottom-full left-0 mb-2 bg-black rounded-lg p-2 max-w-xs">
          <div className="relative">
            <img
              src={preview}
              alt="Preview"
              className="max-w-xs max-h-48 rounded"
            />
            <button
              onClick={() => setPreview(null)}
              className="absolute top-1 right-1 p-1 bg-red-500 rounded-full text-white hover:bg-red-600"
            >
              <X size={12} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
