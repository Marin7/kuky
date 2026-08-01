import { useEffect, useState, type ReactNode } from "react";
import {
  deleteUniversityExam,
  deleteUniversityException,
  deleteUniversityNews,
  deleteUniversitySession,
  getRegisteredUsers,
  getUniversityExamsAdmin,
  getUniversityExceptions,
  getUniversityHomeworkAvailability,
  getUniversityNewsAdmin,
  getUniversityPresentationAvailability,
  getUniversitySessions,
  getUniversityStudents,
  createUniversityException,
  grantUniversityStudent,
  revokeUniversityStudent,
  updateUniversityLevel,
  saveUniversityExam,
  saveUniversityNews,
  saveUniversitySession,
  setUniversityHomeworkAvailability,
  setUniversityPresentationAvailability,
  studentDisplayName,
  type RegisteredUser,
  type UniversityExamAdmin,
  type UniversityLevel,
  type UniversityNewsAdmin,
  type UniversityScheduleSession,
  type UniversityScheduleException,
  type UniversityStudent,
} from "@/lib/admin";

type Section = "students" | "schedule" | "exams" | "news" | "availability";
const levels: UniversityLevel[] = ["BEGINNER", "INTERMEDIATE"];

export function UniversityTab() {
  const [section, setSection] = useState<Section>("students");
  return (
    <div>
      <div className="flex flex-wrap gap-2 border-b pb-4">
        {([
          ["students", "Alumnado"],
          ["schedule", "Horario"],
          ["exams", "Exámenes"],
          ["news", "Noticias"],
          ["availability", "Materiales"],
        ] as const).map(([value, label]) => (
          <button key={value} onClick={() => setSection(value)} className={`rounded-md px-3 py-2 text-sm ${section === value ? "bg-primary text-primary-foreground" : "hover:bg-accent"}`}>{label}</button>
        ))}
      </div>
      <div className="mt-6">
        {section === "students" && <UniversityRoster />}
        {section === "schedule" && <UniversityScheduleAdmin />}
        {section === "exams" && <UniversityExamsAdmin />}
        {section === "news" && <UniversityNewsAdmin />}
        {section === "availability" && <UniversityAvailabilityAdmin />}
      </div>
    </div>
  );
}

function UniversityRoster() {
  const [students, setStudents] = useState<UniversityStudent[]>([]);
  const [users, setUsers] = useState<RegisteredUser[]>([]);
  const [error, setError] = useState<string | null>(null);
  const load = () => Promise.all([getUniversityStudents(), getRegisteredUsers()]).then(([s, u]) => { setStudents(s); setUsers(u); }).catch(() => setError("No se pudo cargar el alumnado."));
  useEffect(() => { void load(); }, []);
  const grant = async (id: string, level: UniversityLevel) => { try { await grantUniversityStudent(id, level); await load(); } catch { setError("No se pudo conceder el acceso."); } };
  const changeLevel = async (id: string, level: UniversityLevel) => { try { await updateUniversityLevel(id, level); await load(); } catch { setError("No se pudo cambiar el nivel."); } };
  const revoke = async (id: string) => { try { await revokeUniversityStudent(id); await load(); } catch { setError("No se pudo retirar el acceso."); } };
  return <div className="space-y-6">
    <p className="text-sm text-muted-foreground">Gestiona el acceso del alumnado universitario. Esta acción no envía correo.</p>
    {error && <p className="text-sm text-destructive">{error}</p>}
    <section><h3 className="font-semibold">Alumnado universitario</h3>
      <ul className="mt-3 divide-y rounded-lg border">{students.map((student) => <li key={student.id} className="flex flex-wrap items-center justify-between gap-3 p-3">
        <span>{studentDisplayName(student)} <span className="text-sm text-muted-foreground">· {student.universityLevel === "BEGINNER" ? "Inicial" : "Intermedio"}</span></span>
        <div className="flex gap-2"><select className="rounded border p-1 text-sm" value={student.universityLevel} onChange={(e) => changeLevel(student.id, e.target.value as UniversityLevel)}>{levels.map((level) => <option key={level} value={level}>{level === "BEGINNER" ? "Inicial" : "Intermedio"}</option>)}</select><button className="text-sm text-destructive underline" onClick={() => revoke(student.id)}>Retirar</button></div>
      </li>)}</ul>
    </section>
    <section><h3 className="font-semibold">Usuarios pendientes</h3>
      <ul className="mt-3 divide-y rounded-lg border">{users.map((user) => <li key={user.id} className="flex flex-wrap items-center justify-between gap-3 p-3"><span>{studentDisplayName(user)}</span><div className="flex gap-2">{levels.map((level) => <button key={level} className="rounded border px-2 py-1 text-xs hover:bg-accent" onClick={() => grant(user.id, level)}>Hacer {level === "BEGINNER" ? "inicial" : "intermedio"}</button>)}</div></li>)}</ul>
    </section>
  </div>;
}

function UniversityScheduleAdmin() {
  const [items, setItems] = useState<UniversityScheduleSession[]>([]);
  const [exceptions, setExceptions] = useState<UniversityScheduleException[]>([]);
  const [level, setLevel] = useState<UniversityLevel>("BEGINNER");
  const [day, setDay] = useState(1);
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("10:30");
  const [title, setTitle] = useState("");
  const load = () =>
    Promise.all([getUniversitySessions(), getUniversityExceptions()]).then(
      ([sessions, scheduleExceptions]) => {
        setItems(sessions);
        setExceptions(scheduleExceptions);
      },
    );
  useEffect(() => { void load(); }, []);
  const create = async () => { await saveUniversitySession({ level, dayOfWeek: day, startTime, endTime, title: title || null }); setTitle(""); await load(); };
  const cancelSession = async (sessionId: string) => {
    await createUniversityException({
      level,
      exceptionDate: new Date().toISOString().slice(0, 10),
      kind: "CANCEL",
      sessionId,
      startTime: null,
      endTime: null,
      title: null,
    });
    await load();
  };
  return <div className="space-y-5"><p className="text-sm text-muted-foreground">Define el horario semanal. Puedes cancelar una sesión para hoy como cambio puntual.</p>
    <div className="flex flex-wrap gap-2 rounded-lg border p-3">
      <select className="rounded border p-2" value={level} onChange={(e) => setLevel(e.target.value as UniversityLevel)}>{levels.map((x) => <option key={x}>{x}</option>)}</select>
      <input className="w-16 rounded border p-2" type="number" min="1" max="7" value={day} onChange={(e) => setDay(Number(e.target.value))} title="Día (1=lunes)" />
      <input className="rounded border p-2" type="time" value={startTime} onChange={(e) => setStartTime(e.target.value)} /><input className="rounded border p-2" type="time" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
      <input className="rounded border p-2" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Título opcional" />
      <button onClick={create} className="rounded bg-primary px-3 py-2 text-sm text-primary-foreground">Añadir</button>
    </div>
    <ul className="divide-y rounded-lg border">{items.map((item) => <li key={item.id} className="flex justify-between gap-3 p-3 text-sm"><span>{item.level} · día {item.dayOfWeek} · {item.startTime}–{item.endTime} {item.title && `· ${item.title}`}</span><button onClick={() => deleteUniversitySession(item.id).then(load)} className="text-destructive underline">Eliminar</button></li>)}</ul>
    <section><h3 className="font-semibold">Cambios puntuales</h3><ul className="mt-3 divide-y rounded-lg border">{exceptions.length ? exceptions.map((item) => <li key={item.id} className="flex justify-between p-3 text-sm"><span>{item.exceptionDate} · {item.kind} {item.title && `· ${item.title}`}</span><button className="text-destructive underline" onClick={() => deleteUniversityException(item.id).then(load)}>Eliminar</button></li>) : <li className="p-3 text-sm text-muted-foreground">No hay cambios puntuales.</li>}</ul></section>
    {items.length > 0 && <div className="text-sm text-muted-foreground">Para cancelar hoy una sesión, selecciona: {items.map((item) => <button key={item.id} onClick={() => cancelSession(item.id)} className="ml-2 text-primary underline">{item.title || `${item.startTime}`}</button>)}</div>}
  </div>;
}

function UniversityExamsAdmin() {
  const [items, setItems] = useState<UniversityExamAdmin[]>([]);
  const [title, setTitle] = useState(""); const [examAt, setExamAt] = useState(""); const [description, setDescription] = useState("");
  const load = () => getUniversityExamsAdmin().then(setItems); useEffect(() => { void load(); }, []);
  const create = async () => { await saveUniversityExam({ title, examAt: new Date(examAt).toISOString(), description: description || null, published: true }); setTitle(""); setDescription(""); await load(); };
  return <CrudList title="Exámenes" form={<><input className="rounded border p-2" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Título" /><input className="rounded border p-2" type="datetime-local" value={examAt} onChange={(e) => setExamAt(e.target.value)} /><input className="rounded border p-2" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Descripción" /><button className="rounded bg-primary px-3 py-2 text-sm text-primary-foreground" onClick={create} disabled={!title || !examAt}>Añadir</button></>} items={items.map((item) => <li key={item.id} className="flex justify-between p-3"><span>{item.title} · {new Date(item.examAt).toLocaleString()}</span><button className="text-destructive underline" onClick={() => deleteUniversityExam(item.id).then(load)}>Eliminar</button></li>)} />;
}

function UniversityNewsAdmin() {
  const [items, setItems] = useState<UniversityNewsAdmin[]>([]);
  const [title, setTitle] = useState(""); const [body, setBody] = useState("");
  const load = () => getUniversityNewsAdmin().then(setItems); useEffect(() => { void load(); }, []);
  const create = async () => { await saveUniversityNews({ title, body, published: true, publishedAt: new Date().toISOString() }); setTitle(""); setBody(""); await load(); };
  return <CrudList title="Noticias" form={<><input className="rounded border p-2" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Título" /><input className="min-w-56 rounded border p-2" value={body} onChange={(e) => setBody(e.target.value)} placeholder="Contenido" /><button className="rounded bg-primary px-3 py-2 text-sm text-primary-foreground" onClick={create} disabled={!title || !body}>Publicar</button></>} items={items.map((item) => <li key={item.id} className="flex justify-between p-3"><span>{item.title}</span><button className="text-destructive underline" onClick={() => deleteUniversityNews(item.id).then(load)}>Eliminar</button></li>)} />;
}

function CrudList({ title, form, items }: { title: string; form: ReactNode; items: ReactNode }) { return <div className="space-y-4"><h3 className="font-semibold">{title}</h3><div className="flex flex-wrap gap-2 rounded-lg border p-3">{form}</div><ul className="divide-y rounded-lg border">{items}</ul></div>; }

function UniversityAvailabilityAdmin() {
  const [level, setLevel] = useState<UniversityLevel>("BEGINNER"); const [homeworks, setHomeworks] = useState(""); const [presentations, setPresentations] = useState(""); const [saved, setSaved] = useState(false);
  useEffect(() => { Promise.all([getUniversityHomeworkAvailability(level), getUniversityPresentationAvailability(level)]).then(([h, p]) => { setHomeworks(h.join(", ")); setPresentations(p.join(", ")); }); }, [level]);
  const ids = (value: string) => value.split(",").map((id) => id.trim()).filter(Boolean);
  const save = async () => { await Promise.all([setUniversityHomeworkAvailability(level, ids(homeworks)), setUniversityPresentationAvailability(level, ids(presentations))]); setSaved(true); };
  return <div className="max-w-xl space-y-4"><p className="text-sm text-muted-foreground">Introduce los IDs de las tareas y presentaciones que estarán disponibles para cada nivel.</p><select className="rounded border p-2" value={level} onChange={(e) => setLevel(e.target.value as UniversityLevel)}>{levels.map((x) => <option key={x}>{x}</option>)}</select><label className="block text-sm font-medium">IDs de tareas<textarea className="mt-1 w-full rounded border p-2 font-mono text-xs" rows={3} value={homeworks} onChange={(e) => setHomeworks(e.target.value)} /></label><label className="block text-sm font-medium">IDs de presentaciones<textarea className="mt-1 w-full rounded border p-2 font-mono text-xs" rows={3} value={presentations} onChange={(e) => setPresentations(e.target.value)} /></label><button onClick={save} className="rounded bg-primary px-3 py-2 text-sm text-primary-foreground">Guardar</button>{saved && <span className="ml-3 text-sm text-green-700">Guardado.</span>}</div>;
}
