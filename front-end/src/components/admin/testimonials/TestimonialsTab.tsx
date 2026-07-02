import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  approveTestimonial,
  deleteTestimonial,
  getAdminTestimonials,
  rejectTestimonial,
  reorderTestimonials,
  unpublishTestimonial,
  updateTestimonialText,
  type AdminTestimonial,
} from "@/lib/admin";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent } from "@/components/ui/card";

export function TestimonialsTab() {
  const { t } = useTranslation();
  const [items, setItems] = useState<AdminTestimonial[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editText, setEditText] = useState("");

  const load = () => {
    setLoading(true);
    getAdminTestimonials()
      .then(setItems)
      .catch(() => setError(t("admin.testimonials.loadError")))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const withErrorHandling = async (action: () => Promise<unknown>) => {
    setError(null);
    try {
      await action();
      load();
    } catch {
      setError(t("admin.testimonials.actionError"));
    }
  };

  const startEdit = (item: AdminTestimonial) => {
    setEditingId(item.id);
    setEditText(item.text);
  };

  const saveEdit = (id: string) =>
    withErrorHandling(async () => {
      await updateTestimonialText(id, editText);
      setEditingId(null);
    });

  const move = (id: string, direction: -1 | 1) => {
    const approved = items.filter((i) => i.status === "APPROVED");
    const index = approved.findIndex((i) => i.id === id);
    const target = index + direction;
    if (target < 0 || target >= approved.length) return;
    const reordered = [...approved];
    [reordered[index], reordered[target]] = [
      reordered[target],
      reordered[index],
    ];
    withErrorHandling(() => reorderTestimonials(reordered.map((i) => i.id)));
  };

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground animate-pulse">
        {t("admin.testimonials.loading")}
      </p>
    );
  }

  const pending = items.filter((i) => i.status === "PENDING");
  const approved = items.filter((i) => i.status === "APPROVED");
  const other = items.filter(
    (i) => i.status === "REJECTED" || i.status === "UNPUBLISHED",
  );

  const renderCard = (item: AdminTestimonial, actions: React.ReactNode) => (
    <Card key={item.id} className="text-sm">
      <CardContent className="pt-4 space-y-2">
        {editingId === item.id ? (
          <div className="space-y-2">
            <Textarea
              value={editText}
              onChange={(e) => setEditText(e.target.value)}
              rows={3}
            />
            <div className="flex gap-2">
              <Button size="sm" onClick={() => saveEdit(item.id)}>
                {t("admin.testimonials.save")}
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => setEditingId(null)}
              >
                {t("admin.testimonials.cancel")}
              </Button>
            </div>
          </div>
        ) : (
          <>
            <p className="font-medium text-foreground">{item.studentName}</p>
            <p className="text-muted-foreground">{item.text}</p>
          </>
        )}
        {editingId !== item.id && (
          <div className="flex flex-wrap gap-2 pt-2">
            {actions}
            <Button size="sm" variant="outline" onClick={() => startEdit(item)}>
              {t("admin.testimonials.edit")}
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );

  return (
    <div className="space-y-8">
      {error && <p className="text-sm text-destructive">{error}</p>}

      <section className="space-y-3">
        <h3 className="font-display text-lg font-semibold">
          {t("admin.testimonials.pendingTitle")}
        </h3>
        {pending.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {t("admin.testimonials.pendingEmpty")}
          </p>
        ) : (
          <div className="space-y-3">
            {pending.map((item) =>
              renderCard(
                item,
                <>
                  <Button
                    size="sm"
                    onClick={() =>
                      withErrorHandling(() => approveTestimonial(item.id))
                    }
                  >
                    {t("admin.testimonials.approve")}
                  </Button>
                  <Button
                    size="sm"
                    variant="destructive"
                    onClick={() =>
                      withErrorHandling(() => rejectTestimonial(item.id))
                    }
                  >
                    {t("admin.testimonials.reject")}
                  </Button>
                </>,
              ),
            )}
          </div>
        )}
      </section>

      <section className="space-y-3">
        <h3 className="font-display text-lg font-semibold">
          {t("admin.testimonials.publishedTitle")}
        </h3>
        {approved.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {t("admin.testimonials.publishedEmpty")}
          </p>
        ) : (
          <div className="space-y-3">
            {approved.map((item) =>
              renderCard(
                item,
                <>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => move(item.id, -1)}
                  >
                    ↑
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => move(item.id, 1)}
                  >
                    ↓
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() =>
                      withErrorHandling(() => unpublishTestimonial(item.id))
                    }
                  >
                    {t("admin.testimonials.unpublish")}
                  </Button>
                  <Button
                    size="sm"
                    variant="destructive"
                    onClick={() =>
                      withErrorHandling(() => deleteTestimonial(item.id))
                    }
                  >
                    {t("admin.testimonials.delete")}
                  </Button>
                </>,
              ),
            )}
          </div>
        )}
      </section>

      {other.length > 0 && (
        <section className="space-y-3">
          <h3 className="font-display text-lg font-semibold">
            {t("admin.testimonials.otherTitle")}
          </h3>
          <div className="space-y-3">
            {other.map((item) =>
              renderCard(
                item,
                <>
                  <Button
                    size="sm"
                    onClick={() =>
                      withErrorHandling(() => approveTestimonial(item.id))
                    }
                  >
                    {t("admin.testimonials.approve")}
                  </Button>
                  <Button
                    size="sm"
                    variant="destructive"
                    onClick={() =>
                      withErrorHandling(() => deleteTestimonial(item.id))
                    }
                  >
                    {t("admin.testimonials.delete")}
                  </Button>
                </>,
              ),
            )}
          </div>
        </section>
      )}
    </div>
  );
}
