// ==============================================================
// SUPABASE SERVER SDK SAMPLE HANDLER
// This module demonstrates how to use the @supabase/server SDK
// and the 'withSupabase' wrapper to create RLS-scoped request handlers.
// ==============================================================

import { withSupabase } from "@supabase/server";

// 1. Example: Fetching categories under the user's RLS scope
export const getCategoriesHandler = {
  fetch: withSupabase({ auth: "user" }, async (_req, ctx) => {
    try {
      // ctx.supabase is RLS-scoped to the authenticated user (waiter/admin)
      const { data, error } = await ctx.supabase
        .from("categories")
        .select("*")
        .order("name", { ascending: true });

      if (error) {
        return Response.json({ error: error.message }, { status: 400 });
      }

      return Response.json(data);
    } catch (err: any) {
      return Response.json({ error: err.message || "Internal server error" }, { status: 500 });
    }
  }),
};

// 2. Example: Creating a user using the admin client (which bypasses RLS)
export const createAdminUserHandler = {
  fetch: withSupabase({ auth: "secret" }, async (req, ctx) => {
    try {
      const body = (await req.json()) as any;
      const username = body.username;
      const role = body.role;

      // ctx.supabaseAdmin has full admin privileges (bypassing RLS)
      const { data, error } = await (ctx.supabaseAdmin as any)
        .from("users")
        .insert([{ username, role }])
        .select()
        .single();

      if (error) {
        return Response.json({ error: error.message }, { status: 400 });
      }

      return Response.json({ message: "User created successfully", user: data });
    } catch (err: any) {
      return Response.json({ error: err.message || "Internal server error" }, { status: 500 });
    }
  }),
};
