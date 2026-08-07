import { useEffect, useState } from "react";
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { GripVertical } from "lucide-react";
import type { UnitContentItem } from "@/lib/admin";
import { Button } from "@/components/ui/button";

function contentKey(item: UnitContentItem): string {
  const id =
    item.type === "PRESENTATION"
      ? item.presentation!.id
      : item.homework!.id;
  return `${item.type}:${id}`;
}

interface SortableRowProps {
  id: string;
  children: React.ReactNode;
  onMoveUp?: () => void;
  onMoveDown?: () => void;
  canMoveUp: boolean;
  canMoveDown: boolean;
  moveUpLabel: string;
  moveDownLabel: string;
}

function SortableRow({
  id,
  children,
  onMoveUp,
  onMoveDown,
  canMoveUp,
  canMoveDown,
  moveUpLabel,
  moveDownLabel,
}: SortableRowProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.7 : 1,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className="flex items-start gap-1 rounded-md border border-transparent bg-muted/40"
    >
      <button
        type="button"
        className="mt-2 shrink-0 cursor-grab touch-none px-1 text-muted-foreground active:cursor-grabbing"
        aria-label="Drag"
        {...attributes}
        {...listeners}
      >
        <GripVertical className="h-4 w-4" />
      </button>
      <div className="min-w-0 flex-1">{children}</div>
      <div className="flex shrink-0 flex-col gap-0.5 pr-1 pt-1">
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="h-5 px-1 text-[10px]"
          disabled={!canMoveUp}
          onClick={onMoveUp}
          aria-label={moveUpLabel}
        >
          ▲
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          className="h-5 px-1 text-[10px]"
          disabled={!canMoveDown}
          onClick={onMoveDown}
          aria-label={moveDownLabel}
        >
          ▼
        </Button>
      </div>
    </div>
  );
}

interface Props {
  items: UnitContentItem[];
  onReorder: (items: UnitContentItem[]) => void | Promise<void>;
  renderItem: (item: UnitContentItem) => React.ReactNode;
  moveUpLabel: string;
  moveDownLabel: string;
}

export function UnitContentSortableList({
  items,
  onReorder,
  renderItem,
  moveUpLabel,
  moveDownLabel,
}: Props) {
  const [local, setLocal] = useState(items);

  useEffect(() => {
    setLocal(items);
  }, [items]);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const ids = local.map(contentKey);

  const persist = async (next: UnitContentItem[]) => {
    setLocal(next);
    await onReorder(next);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;
    const oldIndex = ids.indexOf(String(active.id));
    const newIndex = ids.indexOf(String(over.id));
    if (oldIndex < 0 || newIndex < 0) return;
    void persist(arrayMove(local, oldIndex, newIndex));
  };

  const move = (index: number, delta: number) => {
    const nextIndex = index + delta;
    if (nextIndex < 0 || nextIndex >= local.length) return;
    void persist(arrayMove(local, index, nextIndex));
  };

  if (local.length === 0) return null;

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragEnd={handleDragEnd}
    >
      <SortableContext items={ids} strategy={verticalListSortingStrategy}>
        <div className="space-y-2">
          {local.map((item, index) => {
            const id = contentKey(item);
            return (
              <SortableRow
                key={id}
                id={id}
                canMoveUp={index > 0}
                canMoveDown={index < local.length - 1}
                onMoveUp={() => move(index, -1)}
                onMoveDown={() => move(index, 1)}
                moveUpLabel={moveUpLabel}
                moveDownLabel={moveDownLabel}
              >
                {renderItem(item)}
              </SortableRow>
            );
          })}
        </div>
      </SortableContext>
    </DndContext>
  );
}
