import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";
import { getMe, type UserResponse } from "@/lib/auth";
import { ResourcesView } from "@/components/resources/ResourcesView";
import { MyPurchases } from "@/components/resources/MyPurchases";

export const Route = createFileRoute("/recursos")({
  head: () => ({
    meta: [
      { title: "Recursos — Español con Paula" },
      {
        name: "description",
        content:
          "Explora recursos didácticos para enseñar español. Fichas, guías y materiales de práctica.",
      },
    ],
  }),
  component: RecursosPage,
});

function RecursosPage() {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const catalogRefreshRef = useRef<(() => void) | null>(null);
  const myPurchasesRefreshRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setAuthLoading(false));
  }, []);

  return (
    <div>
      <ResourcesView
        onRefreshRef={catalogRefreshRef}
        onPurchaseSuccess={() => myPurchasesRefreshRef.current?.()}
      />
      {!authLoading && user && (
        <MyPurchases
          onRefreshRef={myPurchasesRefreshRef}
          onCatalogRefresh={() => catalogRefreshRef.current?.()}
        />
      )}
    </div>
  );
}
