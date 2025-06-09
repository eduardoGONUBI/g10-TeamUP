// src/pages/Leaderboard.tsx
import React, { useEffect, useState } from "react";
import "./Leaderboard.css";

interface LeaderboardRow {
  rank: number;
  user_id: number;
  level: number;
  xp: number;
}

interface Meta {
  current_page: number;
  last_page: number;
  per_page: number;
  total: number;
}

export default function Leaderboard() {
  const [rows, setRows] = useState<LeaderboardRow[]>([]);
  const [meta, setMeta] = useState<Meta | null>(null);
  const [page, setPage] = useState(1);

  useEffect(() => {
    fetch(`/api/leaderboard?per_page=15&page=${page}`)
      .then((res) => res.json())
      .then(({ data, meta }) => {
        setRows(data);
        setMeta(meta);
      })
      .catch(console.error);
  }, [page]);

  return (
    <div className="leaderboard-container">
      <h2 className="leaderboard-title">🏆 Leaderboard</h2>
      <table className="leaderboard-table">
        <thead>
          <tr>
            <th>#</th>
            <th>User ID</th>
            <th>Level</th>
            <th>XP</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.user_id}>
              <td className="rank">{r.rank}</td>
              <td className="user">{r.user_id}</td>
              <td className="level">{r.level}</td>
              <td className="xp">{r.xp}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {meta && (
        <div className="pagination">
          <button onClick={() => setPage(p => Math.max(1, p - 1))}
                  disabled={page <= 1}>
            ‹ Prev
          </button>
          <span>{meta.current_page} / {meta.last_page}</span>
          <button onClick={() => setPage(p => Math.min(meta.last_page, p + 1))}
                  disabled={page >= meta.last_page}>
            Next ›
          </button>
        </div>
      )}
    </div>
);
}
