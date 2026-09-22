// src/app/page.tsx
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";

export default async function RootPage() {
  const user = await getSession();
  redirect(user ? "/dashboard" : "/login");
}