import { Routes } from '@angular/router';
import { roleGuard } from './core/auth/role.guard';

const SECRETARIA_ADMIN = ['SECRETARIA', 'ADMIN'];
const ALUNO = ['ALUNO'];
const ADMIN = ['ADMIN'];

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'turmas' },

  {
    path: 'turmas',
    loadComponent: () => import('./features/turma/turma-lista.component').then((m) => m.TurmaListaComponent),
    canActivate: [roleGuard],
  },
  {
    path: 'turmas/novo',
    loadComponent: () => import('./features/turma/turma-form.component').then((m) => m.TurmaFormComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },
  {
    path: 'turmas/:id/editar',
    loadComponent: () => import('./features/turma/turma-form.component').then((m) => m.TurmaFormComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },
  {
    path: 'turmas/:id/matriculas',
    loadComponent: () =>
      import('./features/matricula/matriculas-por-turma.component').then((m) => m.MatriculasPorTurmaComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },

  {
    path: 'minhas-matriculas',
    loadComponent: () =>
      import('./features/matricula/minhas-matriculas.component').then((m) => m.MinhasMatriculasComponent),
    canActivate: [roleGuard],
    data: { papeis: ALUNO },
  },
  {
    path: 'meu-perfil',
    loadComponent: () => import('./features/aluno/meu-perfil.component').then((m) => m.MeuPerfilComponent),
    canActivate: [roleGuard],
    data: { papeis: ALUNO },
  },

  {
    path: 'cursos',
    loadComponent: () => import('./features/curso/curso-lista.component').then((m) => m.CursoListaComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },
  {
    path: 'cursos/novo',
    loadComponent: () => import('./features/curso/curso-form.component').then((m) => m.CursoFormComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },
  {
    path: 'cursos/:id/editar',
    loadComponent: () => import('./features/curso/curso-form.component').then((m) => m.CursoFormComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },

  {
    path: 'disciplinas',
    loadComponent: () =>
      import('./features/disciplina/disciplina-lista.component').then((m) => m.DisciplinaListaComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },
  {
    path: 'disciplinas/novo',
    loadComponent: () =>
      import('./features/disciplina/disciplina-form.component').then((m) => m.DisciplinaFormComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },
  {
    path: 'disciplinas/:id/editar',
    loadComponent: () =>
      import('./features/disciplina/disciplina-form.component').then((m) => m.DisciplinaFormComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },

  {
    path: 'alunos',
    loadComponent: () => import('./features/aluno/aluno-lista.component').then((m) => m.AlunoListaComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },
  {
    path: 'alunos/novo',
    loadComponent: () => import('./features/aluno/aluno-form.component').then((m) => m.AlunoFormComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },
  {
    path: 'alunos/:id/editar',
    loadComponent: () => import('./features/aluno/aluno-form.component').then((m) => m.AlunoFormComponent),
    canActivate: [roleGuard],
    data: { papeis: SECRETARIA_ADMIN },
  },

  {
    path: 'administracao/usuarios',
    loadComponent: () =>
      import('./features/administracao/usuarios-lista.component').then((m) => m.UsuariosListaComponent),
    canActivate: [roleGuard],
    data: { papeis: ADMIN },
  },

  {
    path: 'acesso-negado',
    loadComponent: () => import('./core/auth/acesso-negado.component').then((m) => m.AcessoNegadoComponent),
  },
  { path: '**', redirectTo: 'turmas' },
];
