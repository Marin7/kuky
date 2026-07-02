import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getTestimonials, type Testimonial } from "@/lib/testimonials";

export function TestimonialsSection() {
  const { t } = useTranslation();
  const [testimonials, setTestimonials] = useState<Testimonial[]>([]);

  useEffect(() => {
    getTestimonials()
      .then(setTestimonials)
      .catch(() => setTestimonials([]));
  }, []);

  if (testimonials.length === 0) return null;

  return (
    <section className="mx-auto max-w-6xl px-6 py-16">
      <h2 className="text-center font-display text-3xl font-semibold md:text-4xl">
        {t("home.testimonials.title")}
      </h2>
      <div className="mt-10 grid gap-6 md:grid-cols-3">
        {testimonials.map((testimonial, index) => (
          <figure
            key={index}
            className="rounded-xl border border-border bg-card p-6"
          >
            <blockquote className="text-sm text-muted-foreground">
              “{testimonial.text}”
            </blockquote>
            <figcaption className="mt-4 text-sm font-medium">
              {testimonial.studentName}
            </figcaption>
          </figure>
        ))}
      </div>
    </section>
  );
}
