import type { AdminQuestion, AdminOption, QuestionKind } from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const KIND_OPTIONS: { value: QuestionKind; label: string }[] = [
  { value: "SINGLE_CHOICE", label: "Opción única" },
  { value: "MULTI_CHOICE", label: "Opción múltiple" },
  { value: "FILL_BLANK", label: "Rellenar el hueco" },
];

function defaultOptions(kind: QuestionKind): AdminOption[] {
  if (kind === "FILL_BLANK") return [{ label: "", correct: true }];
  return [
    { label: "", correct: false },
    { label: "", correct: false },
  ];
}

interface Props {
  index: number;
  question: AdminQuestion;
  count: number;
  onChange: (q: AdminQuestion) => void;
  onRemove: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
}

export function QuestionEditorCard({
  index,
  question,
  count,
  onChange,
  onRemove,
  onMoveUp,
  onMoveDown,
}: Props) {
  const setKind = (kind: QuestionKind) => {
    onChange({ ...question, kind, options: defaultOptions(kind) });
  };

  const setPrompt = (prompt: string) => onChange({ ...question, prompt });

  const setOption = (i: number, patch: Partial<AdminOption>) => {
    const options = question.options.map((o, idx) =>
      idx === i ? { ...o, ...patch } : o,
    );
    onChange({ ...question, options });
  };

  // Single-correct: selecting one option clears the others.
  const setSingleCorrect = (i: number) => {
    const options = question.options.map((o, idx) => ({
      ...o,
      correct: idx === i,
    }));
    onChange({ ...question, options });
  };

  const addOption = () =>
    onChange({
      ...question,
      options: [...question.options, { label: "", correct: false }],
    });

  const removeOption = (i: number) =>
    onChange({
      ...question,
      options: question.options.filter((_, idx) => idx !== i),
    });

  const isFillBlank = question.kind === "FILL_BLANK";
  const singleCorrectIndex = question.options.findIndex((o) => o.correct);

  return (
    <div className="space-y-3 rounded-lg border bg-card p-4">
      <div className="flex items-center justify-between gap-2">
        <span className="text-sm font-semibold">Pregunta {index + 1}</span>
        <div className="flex items-center gap-1">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs"
            disabled={index === 0}
            onClick={onMoveUp}
          >
            ↑
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs"
            disabled={index === count - 1}
            onClick={onMoveDown}
          >
            ↓
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs text-destructive"
            onClick={onRemove}
          >
            Eliminar
          </Button>
        </div>
      </div>

      <div className="grid gap-3 sm:grid-cols-[180px_1fr]">
        <div className="space-y-1">
          <Label>Tipo</Label>
          <Select
            value={question.kind}
            onValueChange={(v) => setKind(v as QuestionKind)}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {KIND_OPTIONS.map((o) => (
                <SelectItem key={o.value} value={o.value}>
                  {o.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <Label>Enunciado</Label>
          <Textarea
            value={question.prompt}
            onChange={(e) => setPrompt(e.target.value)}
            rows={2}
            placeholder="Ej. El plural de «el lápiz»"
          />
        </div>
      </div>

      {isFillBlank ? (
        <div className="space-y-2">
          <Label>Respuestas aceptadas</Label>
          <p className="text-xs text-muted-foreground">
            El alumno acierta si su respuesta coincide con alguna (sin
            distinguir mayúsculas, pero respetando los acentos).
          </p>
          {question.options.map((o, i) => (
            <div key={i} className="flex items-center gap-2">
              <Input
                value={o.label}
                onChange={(e) =>
                  setOption(i, { label: e.target.value, correct: true })
                }
                placeholder="Respuesta aceptada"
              />
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="h-8 px-2 text-xs text-destructive"
                disabled={question.options.length === 1}
                onClick={() => removeOption(i)}
              >
                ✕
              </Button>
            </div>
          ))}
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-8 text-xs"
            onClick={addOption}
          >
            Añadir respuesta
          </Button>
        </div>
      ) : (
        <div className="space-y-2">
          <Label>Opciones</Label>
          <p className="text-xs text-muted-foreground">
            {question.kind === "SINGLE_CHOICE"
              ? "Marca la única opción correcta."
              : "Marca todas las opciones correctas."}
          </p>
          {question.kind === "SINGLE_CHOICE" ? (
            <RadioGroup
              value={singleCorrectIndex >= 0 ? String(singleCorrectIndex) : ""}
              onValueChange={(v) => setSingleCorrect(Number(v))}
            >
              {question.options.map((o, i) => (
                <div key={i} className="flex items-center gap-2">
                  <RadioGroupItem value={String(i)} id={`q${index}-o${i}`} />
                  <Input
                    value={o.label}
                    onChange={(e) => setOption(i, { label: e.target.value })}
                    placeholder={`Opción ${i + 1}`}
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-8 px-2 text-xs text-destructive"
                    disabled={question.options.length <= 2}
                    onClick={() => removeOption(i)}
                  >
                    ✕
                  </Button>
                </div>
              ))}
            </RadioGroup>
          ) : (
            question.options.map((o, i) => (
              <div key={i} className="flex items-center gap-2">
                <Checkbox
                  checked={o.correct}
                  onCheckedChange={(c) => setOption(i, { correct: c === true })}
                />
                <Input
                  value={o.label}
                  onChange={(e) => setOption(i, { label: e.target.value })}
                  placeholder={`Opción ${i + 1}`}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="h-8 px-2 text-xs text-destructive"
                  disabled={question.options.length <= 2}
                  onClick={() => removeOption(i)}
                >
                  ✕
                </Button>
              </div>
            ))
          )}
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-8 text-xs"
            onClick={addOption}
          >
            Añadir opción
          </Button>
        </div>
      )}
    </div>
  );
}
