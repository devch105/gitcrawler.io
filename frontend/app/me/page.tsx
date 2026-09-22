"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Loader2, LogOut } from "lucide-react";
import { FaGithub } from "react-icons/fa";
import { api, type User } from "@/lib/api";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function MePage() {
  const router = useRouter();

  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadUser = async () => {
      try {
        const currentUser = await api.me();

        setUser(currentUser);
      } catch (error) {
        console.error("Failed to load user:", error);

        router.replace("/login");
      } finally {
        setLoading(false);
      }
    };

    loadUser();
  }, [router]);

  const handleLogout = async () => {
    try {
      await api.logout();

      router.replace("/login");
    } catch (error) {
      console.error("Logout failed:", error);
    }
  };

  if (loading) {
    return (
      <main className="min-h-screen flex items-center justify-center">
        <div className="flex items-center gap-2 text-muted-foreground">
          <Loader2 className="h-5 w-5 animate-spin" />
          Loading profile...
        </div>
      </main>
    );
  }

  if (!user) {
    return null;
  }

  return (
    <main className="min-h-screen flex items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="mx-auto mb-4">
            {user.avatarUrl ? (
              <img
                src={user.avatarUrl}
                alt={user.displayName}
                className="h-20 w-20 rounded-full border"
              />
            ) : (
              <div className="flex h-20 w-20 items-center justify-center rounded-full bg-muted">
                <FaGithub className="h-10 w-10" />
              </div>
            )}
          </div>

          <CardTitle className="text-2xl">
            Welcome, {user.displayName}
          </CardTitle>
        </CardHeader>

        <CardContent className="space-y-4">
          <div className="rounded-lg border p-4 space-y-2">
            <div>
              <p className="text-xs text-muted-foreground">
                GitHub Username
              </p>

              <p className="font-medium">
                @{user.githubUsername}
              </p>
            </div>

            <div>
              <p className="text-xs text-muted-foreground">
                GitHub ID
              </p>

              <p className="font-medium">
                {user.githubId}
              </p>
            </div>

            <div>
              <p className="text-xs text-muted-foreground">
                User ID
              </p>

              <p className="font-medium break-all">
                {user.id}
              </p>
            </div>
          </div>

          <Button
            variant="outline"
            className="w-full"
            onClick={handleLogout}
          >
            <LogOut className="mr-2 h-4 w-4" />
            Logout
          </Button>
        </CardContent>
      </Card>
    </main>
  );
}