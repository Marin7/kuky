import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  getMyTestimonial,
  submitTestimonial,
  type MyTestimonial as MyTestimonialData,
} from "@/lib/testimonials";
import type { ApiError } from "@/lib/testimonials";
import { Card, CardContent } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { StudentOnlyNotice } from "@/components/StudentOnlyNotice";

export function MyTestimonial() {
  const { t } = useTranslation();
  const [current, setCurrent] = useState<MyTestimonialData | undefined>();
  const [loading, setLoading] = useState(true);
  const [forbidden, setForbidden] = useState(false);
  const [text, setText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    getMyTestimonial()
      .then((data) => {
        setCurrent(data);
        setText(data?.text ?? "");
      })
      .catch((err: ApiError) => {
        if (err.error === "ACCESS_DENIED") setForbidden(true);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  if (forbidden) return <StudentOnlyNotice />;
  if (loading) return null;

  const handleSubmit = () => {
    setSubmitting(true);
    setError(null);
    submitTestimonial(text)
      .then((updated) => setCurrent(updated))
      .catch(() => setError(t("learning.testimonial.error")))
      .finally(() => setSubmitting(false));
  };

  return (
    <section className="space-y-4">
      <h2 className="font-display text-xl font-bold text-foreground">
        {t("learning.testimonial.title")}
      </h2>
      <Card>
        <CardContent className="pt-4 space-y-3">
          {current && (
            <p className="text-sm text-muted-foreground">
              {t("learning.testimonial.status")}:{" "}
              <span className="font-medium text-foreground">
                {t(`learning.testimonial.statuses.${current.status}`)}
              </span>
            </p>
          )}
          <Textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder={t("learning.testimonial.placeholder")}
            rows={4}
          />
          {error && <p className="text-sm text-destructive">{error}</p>}
          <Button
            onClick={handleSubmit}
            disabled={submitting || text.trim().length < 10}
          >
            {current
              ? t("learning.testimonial.resubmit")
              : t("learning.testimonial.submit")}
          </Button>
        </CardContent>
      </Card>
    </section>
  );
}
