import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ShellExecComponent } from './shell-exec.component';

describe('ShellExecComponent', () => {
  let component: ShellExecComponent;
  let fixture: ComponentFixture<ShellExecComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ShellExecComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ShellExecComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
