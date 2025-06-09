// src/pages/Leaderboard.tsx
import React, { useEffect, useState } from "react";
import { DataGrid, type GridColDef, type GridPaginationModel } from "@mui/x-data-grid";
import { Box, Typography } from "@mui/material";

interface Row {
  id: number;           // DataGrid needs an “id” field → we’ll copy rank
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
  const [rows, setRows]   = useState<Row[]>([]);
  const [meta, setMeta]   = useState<Meta | null>(null);
  const [pageSize, setPS] = useState(15);      // synced with backend
  const [page, setPage]   = useState(0);       // 0-based for DataGrid

  /* ------------------------------------------------------------------ */
  /* fetch page every time `page` or `pageSize` changes                 */
  /* ------------------------------------------------------------------ */
  useEffect(() => {
    const backendPage = page + 1;              // backend is 1-based
    fetch(`/api/leaderboard?per_page=${pageSize}&page=${backendPage}`)
      .then(r => r.json())
      .then(({ data, meta }) => {
        // DataGrid expects `id` prop
        setRows(data.map((r: any) => ({ ...r, id: r.rank })));
        setMeta(meta);
      })
      .catch(console.error);
  }, [page, pageSize]);

  /* ------------------------------------------------------------------ */
  /* Column definitions – you can style / format as you wish            */
  /* ------------------------------------------------------------------ */
  const columns: GridColDef[] = [
    { field: "rank",    headerName: "#",       width: 80 },
    { field: "user_id", headerName: "User ID", flex: 1 },
    { field: "level",   headerName: "Level",   width: 120, align: "right", headerAlign: "right" },
    { field: "xp",      headerName: "XP",      width: 120, align: "right", headerAlign: "right" },
  ];

  /* ------------------------------------------------------------------ */
  return (
    <Box sx={{ maxWidth: 800, mx: "auto", mt: 4 }}>
      <Typography variant="h4" align="center" gutterBottom>
        🏆 Leaderboard
      </Typography>

      <DataGrid
        columns={columns}
        rows={rows}
        autoHeight
        pageSizeOptions={[15, 30, 50]}
        paginationModel={{ pageSize, page }}
        rowCount={meta?.total ?? 0}
        paginationMode="server"
        onPaginationModelChange={(model: GridPaginationModel) => {
          setPS(model.pageSize);
          setPage(model.page);
        }}
        disableRowSelectionOnClick
        sx={{
          "& .MuiDataGrid-columnHeaders":  { bgcolor: "#f5f5f5" },
          "& .MuiDataGrid-row:hover":      { bgcolor: "#fafafa" },
        }}
      />
    </Box>
  );
}
