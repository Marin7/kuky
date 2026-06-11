import { Link } from "@tanstack/react-router";
import { studentDisplayName, type Student } from "@/lib/admin";

interface StudentLinkProps {
  student: Pick<
    Student,
    "id" | "email" | "firstName" | "lastName" | "username"
  >;
  showEmail?: boolean;
}

export function StudentLink({ student, showEmail = false }: StudentLinkProps) {
  const name = studentDisplayName(student);
  const hasRealName =
    name !== student.email.split("@")[0] && name !== student.email;

  return (
    <Link
      to="/panel/alumnos/$studentId"
      params={{ studentId: student.id }}
      className="text-primary hover:underline font-medium"
    >
      {name}
      {showEmail && hasRealName && (
        <span className="ml-1 text-muted-foreground font-normal">
          ({student.email})
        </span>
      )}
    </Link>
  );
}
