import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ShellDbComponent } from './shell-db.component';

describe('ShellDbComponent', () => {
  let component: ShellDbComponent;
  let fixture: ComponentFixture<ShellDbComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ShellDbComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ShellDbComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
