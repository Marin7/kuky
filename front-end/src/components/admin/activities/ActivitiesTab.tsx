import { ActivityAdminList } from "./ActivityAdminList";
import { ActivityReviewQueue } from "./ActivityReviewQueue";

export function ActivitiesTab() {
  return (
    <div>
      <ActivityReviewQueue />
      <ActivityAdminList />
    </div>
  );
}
