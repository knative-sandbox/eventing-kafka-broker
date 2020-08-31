/*
 * Copyright 2020 The Knative Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1alpha1

import (
	"context"
	"fmt"

	"knative.dev/pkg/apis"
)

func (ks *KafkaSink) Validate(ctx context.Context) *apis.FieldError {
	var errs *apis.FieldError

	// validate spec
	errs = errs.Also(ks.Spec.Validate(ctx).ViaField("spec"))

	// check immutable fields
	if apis.IsInUpdate(ctx) {
		original := apis.GetBaseline(ctx).(*KafkaSink)
		errs = errs.Also(ks.CheckImmutableFields(ctx, original))
	}

	return errs
}

func (kss *KafkaSinkSpec) Validate(ctx context.Context) *apis.FieldError {
	var errs *apis.FieldError

	// check content mode value
	if kss.ContentMode != nil && !allowedContentModes.Has(*kss.ContentMode) {
		errs = errs.Also(apis.ErrInvalidValue(*kss.ContentMode, "contentMode"))
	}

	return errs
}

func (ks *KafkaSink) CheckImmutableFields(ctx context.Context, original *KafkaSink) *apis.FieldError {

	var errs *apis.FieldError

	errs = errs.Also(ks.Spec.CheckImmutableFields(ctx, &original.Spec))

	return errs
}

func (kss *KafkaSinkSpec) CheckImmutableFields(ctx context.Context, original *KafkaSinkSpec) *apis.FieldError {

	var errs *apis.FieldError

	if kss.ReplicationFactor != original.ReplicationFactor {
		errs = errs.Also(ErrImmutableField("replicationFactor"))
	}

	if kss.BootstrapServers != original.BootstrapServers {
		errs = errs.Also(ErrImmutableField("bootstrapServers"))
	}

	if kss.NumPartitions != original.NumPartitions {
		errs = errs.Also(ErrImmutableField("numPartitions"))
	}

	return errs
}

func ErrImmutableField(field string) *apis.FieldError {
	return &apis.FieldError{
		Message: fmt.Sprintf("Immutable field %s updated", field),
		Paths:   []string{field},
	}
}
