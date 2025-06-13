
import React, { useEffect, useState } from "react";
 //biblioteca externa usada
import { DataGrid, type GridColDef, type GridPaginationModel } from "@mui/x-data-grid";  
import { Box, Typography } from "@mui/material";  

// linhas da tabela
interface Row {
  id: number;          
  rank: number;
  user_id: number;
  level: number;
  xp: number;
}

// metadata da paginaçao
interface Meta {
  current_page: number;
  last_page: number;
  per_page: number;
  total: number;
}


export default function Leaderboard() {
  // ----------- estado ----------------------
  const [rows, setRows]   = useState<Row[]>([]);
  const [meta, setMeta]   = useState<Meta | null>(null);
  const [pageSize, setPS] = useState(15);      
  const [page, setPage]   = useState(0);       


  useEffect(() => {        // carrega dados da api sempre que muda de pagina
    const backendPage = page + 1;           
    fetch(`/api/leaderboard?per_page=${pageSize}&page=${backendPage}`)
      .then(r => r.json())
      .then(({ data, meta }) => {
        // DataGrid expects `id` prop
        setRows(data.map((r: any) => ({ ...r, id: r.rank })));
        setMeta(meta);
      })
      .catch(console.error);
  }, [page, pageSize]);


// colunas da tabela
  const columns: GridColDef[] = [
    { field: "rank",    headerName: "#",       width: 80 },
    { field: "user_id", headerName: "User ID", flex: 1 },
    { field: "level",   headerName: "Level",   width: 120, align: "right", headerAlign: "right" },
    { field: "xp",      headerName: "XP",      width: 120, align: "right", headerAlign: "right" },
  ];


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
