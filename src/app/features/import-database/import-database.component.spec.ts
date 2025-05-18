import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImportDatabaseComponent } from './import-database.component';

describe('ImportDatabaseComponent', () => {
  let component: ImportDatabaseComponent;
  let fixture: ComponentFixture<ImportDatabaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ImportDatabaseComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ImportDatabaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
