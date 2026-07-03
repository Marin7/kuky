import { HomeworkAdminList } from "./HomeworkAdminList";
import { HomeworkReviewQueue } from "./HomeworkReviewQueue";

export function HomeworkTab() {
  return (
    <div>
      <HomeworkReviewQueue />
      <HomeworkAdminList />
    </div>
  );
}
