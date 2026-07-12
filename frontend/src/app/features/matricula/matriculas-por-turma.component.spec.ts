import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatriculasPorTurmaComponent } from './matriculas-por-turma.component';
import { MatriculaService } from './matricula.service';
import { TurmaService } from '../turma/turma.service';
import { MatriculaResponse } from '../../shared/models/matricula.model';
import { TurmaResponse } from '../../shared/models/turma.model';

describe('MatriculasPorTurmaComponent', () => {
  let matriculaServiceSpy: jasmine.SpyObj<MatriculaService>;
  let turmaServiceSpy: jasmine.SpyObj<TurmaService>;

  const turma: TurmaResponse = {
    id: 1,
    codigo: 'T1',
    cursoId: 1,
    cursoNome: 'Curso 1',
    disciplinaId: 1,
    disciplinaNome: 'Disciplina 1',
    limiteVagas: 1,
    vagasOcupadas: 1,
    status: 'ABERTA',
    ativo: true,
    criadoEm: '2026-01-01T00:00:00Z',
  };

  const matriculaPendente: MatriculaResponse = {
    id: 5,
    alunoId: 2,
    alunoNome: 'Ana Silva',
    turmaId: 1,
    turmaCodigo: 'T1',
    status: 'PENDENTE',
    criadoEm: '2026-01-01T00:00:00Z',
    confirmadoEm: null,
    canceladoEm: null,
  };

  function setup() {
    matriculaServiceSpy = jasmine.createSpyObj('MatriculaService', ['listarPorTurma', 'confirmar']);
    turmaServiceSpy = jasmine.createSpyObj('TurmaService', ['buscarPorId']);
    turmaServiceSpy.buscarPorId.and.resolveTo(turma);
    matriculaServiceSpy.listarPorTurma.and.resolveTo([matriculaPendente]);

    TestBed.configureTestingModule({
      imports: [MatriculasPorTurmaComponent],
      providers: [
        { provide: MatriculaService, useValue: matriculaServiceSpy },
        { provide: TurmaService, useValue: turmaServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } },
        },
      ],
    });

    const fixture = TestBed.createComponent(MatriculasPorTurmaComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('em 409 com errorCode VAGAS_ESGOTADAS, mostra a mensagem específica de vaga perdida (não a genérica)', async () => {
    const fixture = setup();
    matriculaServiceSpy.confirmar.and.rejectWith(
      new HttpErrorResponse({
        status: 409,
        error: { errorCode: 'VAGAS_ESGOTADAS', status: 409, detail: 'Sem vagas disponíveis.' },
      }),
    );

    await fixture.whenStable();
    await fixture.componentInstance.confirmar(matriculaPendente);

    expect(fixture.componentInstance['vagaPerdida']()).toBeTrue();
    expect(fixture.componentInstance['erro']()).toBeNull();
  });

  it('em 409 com errorCode CONFLITO_CONCORRENCIA, também trata como vaga perdida', async () => {
    const fixture = setup();
    matriculaServiceSpy.confirmar.and.rejectWith(
      new HttpErrorResponse({
        status: 409,
        error: { errorCode: 'CONFLITO_CONCORRENCIA', status: 409, detail: 'Conflito.' },
      }),
    );

    await fixture.whenStable();
    await fixture.componentInstance.confirmar(matriculaPendente);

    expect(fixture.componentInstance['vagaPerdida']()).toBeTrue();
  });

  it('em erro que não é vaga perdida (ex: 500), mostra a mensagem genérica, não a de vaga perdida', async () => {
    const fixture = setup();
    matriculaServiceSpy.confirmar.and.rejectWith(new HttpErrorResponse({ status: 500 }));

    await fixture.whenStable();
    await fixture.componentInstance.confirmar(matriculaPendente);

    expect(fixture.componentInstance['vagaPerdida']()).toBeFalse();
    expect(fixture.componentInstance['erro']()).not.toBeNull();
  });

  it('confirmar() com sucesso recarrega a lista de matrículas', async () => {
    const fixture = setup();
    matriculaServiceSpy.confirmar.and.resolveTo({ ...matriculaPendente, status: 'CONFIRMADA' });

    await fixture.whenStable();
    await fixture.componentInstance.confirmar(matriculaPendente);

    expect(matriculaServiceSpy.listarPorTurma).toHaveBeenCalledTimes(2);
    expect(fixture.componentInstance['vagaPerdida']()).toBeFalse();
  });
});
