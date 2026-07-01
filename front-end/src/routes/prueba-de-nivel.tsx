import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { getMe, type UserResponse } from "@/lib/auth";
import {
  getPlacementTest,
  startAttempt,
  getAttemptResult,
  type AttemptResultResponse,
  type PlacementTestResponse,
  type SectionDto,
  type Skill,
} from "@/lib/placement";
import { PlacementIntro } from "@/components/placement/PlacementIntro";
import { PlacementLoginRequired } from "@/components/placement/PlacementLoginRequired";
import { SectionRunner } from "@/components/placement/SectionRunner";
import { PlacementResult } from "@/components/placement/PlacementResult";
import { FullEvaluationPanel } from "@/components/placement/FullEvaluationPanel";
import { seo } from "@/lib/seo";

export const Route = createFileRoute("/prueba-de-nivel")({
  head: () => ({
    meta: seo({
      title: "Prueba de nivel — Español con Paula",
      description: "Descubre tu nivel de español con una prueba gratuita.",
      path: "/prueba-de-nivel",
    }),
  }),
  component: PruebaDeNivelPage,
});

type Stage = "intro" | "section" | "result" | "fullEvaluation";

function PruebaDeNivelPage() {
  const { t } = useTranslation();

  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);

  const [test, setTest] = useState<PlacementTestResponse | null>(null);
  const [attemptId, setAttemptId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [stage, setStage] = useState<Stage>("intro");
  const [currentSkillIndex, setCurrentSkillIndex] = useState(0);
  const [result, setResult] = useState<AttemptResultResponse | null>(null);

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setAuthLoading(false));
  }, []);

  useEffect(() => {
    if (!user) return;
    getPlacementTest()
      .then((data) => {
        setTest(data);
        setAttemptId(data.attemptId);
        const unsubmitted = data.sections.findIndex(
          (s) => s.status !== "SUBMITTED",
        );
        if (data.attemptId && unsubmitted === -1) {
          setStage("result");
        } else if (data.attemptId) {
          setStage("section");
          setCurrentSkillIndex(unsubmitted === -1 ? 0 : unsubmitted);
        }
      })
      .finally(() => setLoading(false));
  }, [user]);

  useEffect(() => {
    if (stage !== "result" || !attemptId) return;
    getAttemptResult(attemptId).then(setResult);
  }, [stage, attemptId]);

  const handleStart = async () => {
    setStarting(true);
    try {
      const { attemptId: id } = await startAttempt();
      setAttemptId(id);
      setStage("section");
      setCurrentSkillIndex(0);
    } finally {
      setStarting(false);
    }
  };

  const handleSectionSubmitted = (skill: Skill) => {
    if (!test) return;
    const next = test.sections.findIndex(
      (s, i) => i > currentSkillIndex && s.status !== "SUBMITTED",
    );
    setTest({
      ...test,
      sections: test.sections.map((s) =>
        s.skill === skill ? { ...s, status: "SUBMITTED" } : s,
      ),
    });
    if (next === -1) {
      setStage("result");
    } else {
      setCurrentSkillIndex(next);
    }
  };

  if (authLoading || (user && loading)) {
    return (
      <div className="mx-auto max-w-2xl px-6 py-16 text-center">
        <p className="animate-pulse text-sm text-muted-foreground">
          {t("common.loading")}
        </p>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="mx-auto max-w-3xl px-6 py-16">
        <PlacementLoginRequired />
      </div>
    );
  }

  if (!test) return null;

  const currentSection: SectionDto | undefined =
    test.sections[currentSkillIndex];

  return (
    <div className="mx-auto max-w-3xl px-6 py-16">
      {stage === "intro" && (
        <PlacementIntro
          sections={test.sections}
          starting={starting}
          onStart={handleStart}
        />
      )}

      {stage === "section" && attemptId && currentSection && (
        <SectionRunner
          key={currentSection.skill}
          attemptId={attemptId}
          section={currentSection}
          onSubmitted={() => handleSectionSubmitted(currentSection.skill)}
        />
      )}

      {stage === "result" && result && (
        <PlacementResult
          result={result}
          onWantFullEvaluation={() => setStage("fullEvaluation")}
        />
      )}

      {stage === "fullEvaluation" && <FullEvaluationPanel />}
    </div>
  );
}
